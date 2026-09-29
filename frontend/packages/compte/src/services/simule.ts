import { ReauthCancelled, type CleSecurite } from '@gimac/socle'
import { ACTIONS_REQUISES, LANGUES_COMPTE, type Compte, type Moi, type SessionCompte } from '../modele'
import type { ServiceMonCompte } from './ports'
import { creerSimulateur, type OptionsSimulateur } from './simulateur'

// Substitut de MS-IAM pour « Mon compte » : mêmes formes de réponse et mêmes codes de refus que le
// service ; ses « règles » ne sont qu'une imitation. La reconnexion simulée exécute les actions requises.

export type OptionsMonCompteSimule = OptionsSimulateur & {
  /** Compte créé par l'administration, jamais connecté : fiche à compléter et actions requises. */
  premierAcces?: boolean
  compte?: Partial<Compte>
  /** L'utilisateur annule la demande de sa clé de sécurité. */
  annulerEnrolement?: boolean
}

const TELEPHONE = /^\+?[0-9 ]{8,20}$/
const il = (heures: number) => new Date(Date.now() - heures * 3_600_000).toISOString()

export function compteDemo(c: Partial<Compte> = {}): Compte {
  return {
    id: 'u-demo', identifiant: 'demo', prenom: 'Aïssatou', nom: 'Ngo Mbarga', nomAffiche: 'Aïssatou Ngo Mbarga', email: 'a.ngombarga@gimac-afr.org',
    emailVerifie: true, actif: true, creeLe: il(24 * 90), telephone: '+237 699 12 34 56', fonction: 'Ingénieure d’exploitation', langue: 'fr',
    institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils: ['ROLE_CHEF_DDRM'], actionsRequises: [], secondFacteur: true, ...c,
  }
}

export function monCompteSimule(o: OptionsMonCompteSimule = {}): ServiceMonCompte {
  const { attendre, refus } = creerSimulateur(o)
  const compte: Compte = compteDemo(
    o.premierAcces
      ? { profilComplet: false, premierAccesEnAttente: true, telephone: null, fonction: null, actionsRequises: ['UPDATE_PASSWORD', 'VERIFY_EMAIL'], emailVerifie: false, ...o.compte }
      : o.compte,
  )
  let cles: CleSecurite[] = o.premierAcces ? [] : [{ id: 'k-0001', nom: 'YubiKey 5 NFC (bureau)', creeeLe: il(24 * 60), derniereUtilisation: il(3) }]
  let sessions: SessionCompte[] = [
    { id: 's-courante', adresseIp: '10.20.1.15', debut: il(1), dernierAcces: il(0), applications: ['gateway'] },
    { id: 's-ancienne', adresseIp: '10.20.4.62', debut: il(26), dernierAcces: il(20), applications: ['gateway'] },
  ]
  let n = 1

  const moi = (): Moi => {
    compte.premierAccesEnAttente = !compte.profilComplet || compte.actionsRequises.length > 0
    const etapes = [...compte.actionsRequises]
    if (!compte.profilComplet) etapes.push('FICHE')
    if (!compte.secondFacteur && !etapes.includes('CONFIGURE_TOTP')) etapes.push('CONFIGURE_TOTP')
    return { compte, clesSecurite: cles.length, sessions: sessions.length, etapesRestantes: etapes }
  }
  const verifierTelephone = (t?: string | null) => {
    if (t && !TELEPHONE.test(t)) throw refus(400, 'IAM_PHONE_INVALID', 'Numéro de téléphone invalide', { telephone: t })
  }
  const verifierLangue = (l?: string | null) => {
    if (!l || !(LANGUES_COMPTE as readonly string[]).includes(l)) throw refus(400, 'IAM_LANGUAGE_INVALID', 'Langue : fr, en ou es')
  }

  return {
    moi: () => attendre(moi),
    premiereConfiguration: (p) =>
      attendre(() => {
        if (!p.charteAcceptee) throw refus(400, 'IAM_CHARTER_REQUIRED', "La charte d'usage doit être acceptée")
        if (!p.telephone?.trim()) throw refus(400, 'IAM_PHONE_REQUIRED', 'Le téléphone professionnel est obligatoire')
        verifierTelephone(p.telephone)
        verifierLangue(p.langue)
        Object.assign(compte, { telephone: p.telephone, langue: p.langue, fonction: p.fonction ?? compte.fonction, profilComplet: true })
        return moi()
      }),
    modifier: (f) =>
      attendre(() => {
        verifierTelephone(f.telephone)
        if (f.langue) verifierLangue(f.langue)
        Object.entries(f).forEach(([k, v]) => {
          if (v !== undefined && v !== null) (compte as Record<string, unknown>)[k] = v
        })
        compte.nomAffiche = `${compte.prenom ?? ''} ${compte.nom ?? ''}`.trim() || compte.identifiant
        return moi()
      }),
    demanderAction: (action) =>
      attendre(() => {
        if (action !== 'UPDATE_PASSWORD' && action !== 'CONFIGURE_TOTP') throw refus(400, 'IAM_ACTION_NOT_ALLOWED', 'Action non disponible en libre-service')
        if (action === 'CONFIGURE_TOTP') compte.secondFacteur = false
        else compte.actionsRequises = ACTIONS_REQUISES.filter((a) => a === action || compte.actionsRequises.includes(a))
        return moi()
      }),
    sessions: () => attendre(() => sessions),
    fermerSession: (id) =>
      attendre(() => {
        sessions = sessions.filter((s) => s.id !== id)
      }),
    fermerSessions: () =>
      attendre(() => {
        sessions = []
      }),
    cles: () => attendre(() => cles),
    enrolerCle: (nom) =>
      attendre(() => {
        if (o.annulerEnrolement) throw new ReauthCancelled()
        if (!nom.trim() || nom.length > 100) throw refus(400, 'IAM_KEY_NAME_INVALID', 'Nom de la clé obligatoire (100 caractères au plus)')
        const cle: CleSecurite = { id: `k-sim-${++n}`, nom: nom.trim(), creeeLe: new Date().toISOString(), derniereUtilisation: null }
        cles = [...cles, cle]
        return cle
      }),
    supprimerCle: (id) =>
      attendre(() => {
        if (!cles.some((c) => c.id === id)) throw refus(404, 'IAM_KEY_NOT_FOUND', 'Ressource introuvable')
        cles = cles.filter((c) => c.id !== id)
      }),
    // Le fournisseur d'identité simulé exécute aussitôt les actions requises.
    reconnecter: () =>
      attendre(() => {
        compte.actionsRequises = []
        compte.secondFacteur = true
        compte.emailVerifie = true
      }),
  }
}
