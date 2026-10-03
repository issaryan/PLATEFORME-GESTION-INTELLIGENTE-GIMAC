import { ApiError, type Problem } from './errors'
import { entetesEcriture, http } from './client'
import { signalerActivite } from '../session/inactivite'
import type { Couche } from './prefixes'

// Dépôts fragmentés (claim check, D-74, docs/lot1/depots-fragmentes.md) : le fichier part en morceaux vers
// MS-DOCUMENT, l'un après l'autre, avec la progression octet par octet ; une coupure reprend aux morceaux
// manquants ; à la fin, le serveur vérifie l'envoi et rend le ticket du document (documentId), seul porté
// ensuite par le message ou l'acte métier. Jamais de fichier entier en mémoire : chaque morceau est une
// tranche (Blob.slice) lue par le navigateur au moment de l'envoi.

export type EtatDepot = 'OUVERT' | 'EN_VERIFICATION' | 'DISPONIBLE' | 'REJETE' | 'ABANDONNE'

export type Depot = {
  id: string
  etat: EtatDepot
  nomFichier: string
  typeMime: string
  taille: number
  taillePartie: number
  nombreParties: number
  partiesRecues: number[]
  documentId?: string | null
  sha256?: string | null
  dedoublonne?: boolean | null
  motif?: string | null
  expireLe?: string | null
}

export type OuvertureDepot = { nomFichier: string; typeMime: string; taille: number; finalite: string; reference: string }

/** Avancement d'un envoi : octets envoyés, puis vérification par le serveur, puis document prêt. */
export type Progression = { phase: 'envoi' | 'verification' | 'pret'; envoye: number; total: number }

/** Résultat d'un envoi : le ticket du document, à porter par le message ou l'acte métier. */
export type Televersement = { documentId: string; sha256: string; dedoublonne: boolean; depotId: string }

export interface ServiceDepots {
  ouvrir(o: OuvertureDepot): Promise<Depot>
  /** Envoie une partie (numérotée à partir de 1) ; surOctets reçoit les octets de cette partie déjà partis. */
  envoyerPartie(id: string, numero: number, partie: Blob, o: { surOctets?: (n: number) => void; signal?: AbortSignal }): Promise<void>
  etat(id: string): Promise<Depot>
  terminer(id: string): Promise<Depot>
  abandonner(id: string): Promise<void>
}

/** Envoi d'octets bruts avec progression (fetch ne la donne pas) : XMLHttpRequest, mêmes en-têtes que le client. */
function envoyerOctets(url: string, corps: Blob, o: { surOctets?: (n: number) => void; signal?: AbortSignal }): Promise<void> {
  return new Promise((resoudre, rejeter) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url)
    xhr.withCredentials = true
    xhr.setRequestHeader('Content-Type', 'application/octet-stream')
    xhr.setRequestHeader('Accept', 'application/json')
    entetesEcriture().forEach((v, k) => xhr.setRequestHeader(k, v))
    xhr.upload.onprogress = (e) => o.surOctets?.(e.loaded)
    const abandon = () => xhr.abort()
    o.signal?.addEventListener('abort', abandon)
    const fin = () => o.signal?.removeEventListener('abort', abandon)
    xhr.onload = () => {
      fin()
      if (xhr.status >= 200 && xhr.status < 300) {
        o.surOctets?.(corps.size)
        resoudre()
        return
      }
      let p: Problem = {}
      try {
        p = JSON.parse(xhr.responseText) as Problem
      } catch {
        /* corps non JSON */
      }
      rejeter(new ApiError(xhr.status, p.code ?? `HTTP_${xhr.status}`, p.message ?? p.detail ?? '', p.correlationId ?? xhr.getResponseHeader('X-Correlation-Id') ?? '', p.details))
    }
    xhr.onerror = () => {
      fin()
      rejeter(new ApiError(0, 'RESEAU_INDISPONIBLE', 'La passerelle est injoignable.', ''))
    }
    xhr.onabort = () => {
      fin()
      rejeter(new DOMException('Envoi annulé', 'AbortError'))
    }
    xhr.send(corps)
  })
}

/** Dépôts de MS-DOCUMENT pour une couche : `/bff/{des|console|portail}/document/depots`. */
export function depotsHttp(couche: Couche): ServiceDepots {
  const D = `/bff/${couche}/document/depots`
  const un = (id: string) => `${D}/${encodeURIComponent(id)}`
  return {
    ouvrir: (o) => http.post<Depot>(D, { body: o }),
    envoyerPartie: (id, numero, partie, o) => envoyerOctets(new URL(`${un(id)}/parties/${numero}`, window.location.origin).toString(), partie, o),
    // Suivi de la vérification : appel de fond, l'envoi lui-même compte comme activité.
    etat: (id) => http.get<Depot>(un(id), { fond: true }),
    terminer: (id) => http.post<Depot>(`${un(id)}/fin`),
    abandonner: (id) => http.delete(un(id)),
  }
}

/** Erreur qui vaut d'être retentée : réseau, passerelle ou service momentanément indisponible. */
const passagere = (e: unknown) => e instanceof ApiError && (e.status === 0 || e.status === 408 || e.status === 429 || e.status >= 500)

const attendre = (ms: number, signal?: AbortSignal) =>
  new Promise<void>((resoudre, rejeter) => {
    const t = window.setTimeout(resoudre, ms)
    signal?.addEventListener('abort', () => {
      window.clearTimeout(t)
      rejeter(new DOMException('Envoi annulé', 'AbortError'))
    })
  })

export type OptionsTeleversement = {
  surProgression?: (p: Progression) => void
  signal?: AbortSignal
  /** Essais par partie avant l'échec (trois par défaut). */
  essais?: number
  /** Intervalle de suivi de la vérification (1 s par défaut) et premier délai entre deux essais. */
  intervalleMs?: number
}

/**
 * Dépose un fichier par morceaux et rend son ticket. Annulation (signal) : le dépôt est abandonné côté
 * serveur. Rejet par la vérification (numéro de carte, taille) : ApiError 422 portant le motif.
 */
export async function televerser(
  service: ServiceDepots,
  fichier: File,
  destination: { finalite: string; reference: string },
  o: OptionsTeleversement = {},
): Promise<Televersement> {
  const intervalle = o.intervalleMs ?? 1000
  const essais = Math.max(1, o.essais ?? 3)
  const total = fichier.size
  let depot = await service.ouvrir({
    nomFichier: fichier.name,
    typeMime: fichier.type || 'application/octet-stream',
    taille: total,
    finalite: destination.finalite,
    reference: destination.reference,
  })
  const tailleDe = (n: number) => Math.min(depot.taillePartie, total - (n - 1) * depot.taillePartie)
  const recues = new Set(depot.partiesRecues)
  let envoye = [...recues].reduce((s, n) => s + tailleDe(n), 0)
  const signaler = (phase: Progression['phase'], enCours = 0) => o.surProgression?.({ phase, envoye: Math.min(total, envoye + enCours), total })
  try {
    signaler('envoi')
    for (let n = 1; n <= depot.nombreParties; n++) {
      if (recues.has(n)) continue
      const debut = (n - 1) * depot.taillePartie
      const partie = fichier.slice(debut, debut + tailleDe(n))
      for (let essai = 1; ; essai++) {
        if (o.signal?.aborted) throw new DOMException('Envoi annulé', 'AbortError')
        try {
          await service.envoyerPartie(depot.id, n, partie, { signal: o.signal, surOctets: (octets) => signaler('envoi', octets) })
          break
        } catch (e) {
          if (!passagere(e) || essai >= essais) throw e
          await attendre(intervalle * 2 ** (essai - 1), o.signal)
          // Reprise : le serveur dit ce qu'il a reçu (la partie a pu arriver malgré l'erreur).
          const etat = await service.etat(depot.id).catch(() => null)
          if (etat?.partiesRecues.includes(n)) break
        }
      }
      recues.add(n)
      envoye += partie.size
      signaler('envoi')
      signalerActivite()
    }
    depot = await service.terminer(depot.id)
    signaler('verification')
    while (depot.etat === 'EN_VERIFICATION') {
      await attendre(intervalle, o.signal)
      depot = await service.etat(depot.id)
      signalerActivite()
    }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') await service.abandonner(depot.id).catch(() => undefined)
    throw e
  }
  if (depot.etat !== 'DISPONIBLE' || !depot.documentId || !depot.sha256) {
    throw new ApiError(422, depot.motif ?? 'DOC_UPLOAD_REJECTED', '', '', { etat: depot.etat })
  }
  signaler('pret')
  return { documentId: depot.documentId, sha256: depot.sha256, dedoublonne: Boolean(depot.dedoublonne), depotId: depot.id }
}
