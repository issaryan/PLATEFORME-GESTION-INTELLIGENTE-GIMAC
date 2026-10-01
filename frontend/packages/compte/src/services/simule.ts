import { ETAPES_PREMIER_ACCES, MOT_DE_PASSE_DEMO, ReauthCancelled, simulationConnexion, violationsMotDePasse, type CleSecurite } from '@gimac/socle'
import { LANGUES_COMPTE, type Compte, type EnrolementTotp, type Moi, type SessionCompte } from '../modele'
import type { ServiceMonCompte } from './ports'
import { creerSimulateur, type OptionsSimulateur } from './simulateur'

// Substitut de MS-IAM pour « Mon compte » : mêmes formes de réponse et mêmes codes de refus que le
// service ; ses « règles » ne sont qu'une imitation. Démonstration (D-65) : le mot de passe actuel est
// MOT_DE_PASSE_DEMO (ou celui choisi pendant la démonstration), tout code TOTP de six chiffres est accepté ;
// le mot de passe choisi et la fin du premier accès sont transmis à la connexion simulée de la page.

export type OptionsMonCompteSimule = OptionsSimulateur & {
  /** Compte créé par l'administration, jamais connecté : étapes du premier accès à faire. */
  premierAcces?: boolean
  /** Étapes du premier accès (MOT_DE_PASSE, SECOND_FACTEUR, FICHE, CHARTE) ; les quatre à défaut. */
  etapes?: string[]
  compte?: Partial<Compte>
  /** Mot de passe actuel ; MOT_DE_PASSE_DEMO à défaut. */
  motDePasse?: string
  /** L'utilisateur annule la demande de sa clé de sécurité. */
  annulerEnrolement?: boolean
}

const TELEPHONE = /^\+?[0-9 ]{8,20}$/
const CODE = /^[0-9]{6}$/
const BASE32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
const il = (heures: number) => new Date(Date.now() - heures * 3_600_000).toISOString()

export function compteDemo(c: Partial<Compte> = {}): Compte {
  return {
    id: 'u-demo', identifiant: 'demo', prenom: 'Aïssatou', nom: 'Ngo Mbarga', nomAffiche: 'Aïssatou Ngo Mbarga', email: 'a.ngombarga@gimac-afr.org',
    emailVerifie: true, actif: true, creeLe: il(24 * 90), telephone: '+237 699 12 34 56', fonction: 'Ingénieure d’exploitation', langue: 'fr',
    institutionId: null, profilComplet: true, premierAccesEnAttente: false, profils: ['ROLE_CHEF_DDRM'], actionsRequises: [], secondFacteur: true, ...c,
  }
}

/** Secret base32 de 32 caractères (160 bits), comme MS-IAM ; démonstration seulement. */
function secretDemo(): string {
  const octets = crypto.getRandomValues(new Uint8Array(32))
  return Array.from(octets, (o) => BASE32[o % 32]).join('')
}

export function monCompteSimule(o: OptionsMonCompteSimule = {}): ServiceMonCompte {
  const { attendre, refus } = creerSimulateur(o)
  const restantes = new Set<string>(o.premierAcces ? (o.etapes ?? ETAPES_PREMIER_ACCES) : [])
  const compte: Compte = compteDemo(
    o.premierAcces
      ? {
          ...(restantes.has('FICHE') ? { profilComplet: false, telephone: null, fonction: null } : {}),
          ...(restantes.has('SECOND_FACTEUR') ? { secondFacteur: false } : {}),
          premierAccesEnAttente: true, emailVerifie: false, ...o.compte,
        }
      : o.compte,
  )
  if (!o.premierAcces) {
    if (!compte.profilComplet) restantes.add('FICHE')
    if (!compte.secondFacteur) restantes.add('SECOND_FACTEUR')
  }
  let motDePasse = o.motDePasse ?? MOT_DE_PASSE_DEMO
  let cles: CleSecurite[] = o.premierAcces ? [] : [{ id: 'k-0001', nom: 'YubiKey 5 NFC (bureau)', creeeLe: il(24 * 60), derniereUtilisation: il(3) }]
  let sessions: SessionCompte[] = [
    { id: 's-courante', adresseIp: '10.20.1.15', debut: il(1), dernierAcces: il(0), applications: ['gateway'] },
    { id: 's-ancienne', adresseIp: '10.20.4.62', debut: il(26), dernierAcces: il(20), applications: ['gateway'] },
  ]
  const enrolements = new Map<string, { secret: string; expireA: number }>()
  let n = 1

  const retirer = (...etapes: string[]) => {
    etapes.forEach((e) => restantes.delete(e))
    if (o.premierAcces && restantes.size === 0) simulationConnexion.terminerPremierAcces(compte.identifiant)
  }
  const moi = (): Moi => {
    compte.premierAccesEnAttente = restantes.size > 0
    compte.actionsRequises = [...restantes].filter((e) => e === 'MOT_DE_PASSE' || e === 'SECOND_FACTEUR')
    return { compte, clesSecurite: cles.length, sessions: sessions.length, etapesRestantes: [...restantes] }
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
        retirer('FICHE', 'CHARTE')
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
    changerMotDePasse: (c) =>
      attendre(() => {
        if (c.actuel !== motDePasse) throw refus(400, 'IAM_PASSWORD_INVALID', 'Mot de passe actuel incorrect')
        if (c.nouveau === motDePasse) throw refus(400, 'IAM_PASSWORD_REUSED', 'Mot de passe déjà utilisé')
        const violations = violationsMotDePasse(c.nouveau, compte.identifiant)
        if (violations.length) throw refus(400, 'IAM_PASSWORD_POLICY', 'Mot de passe non conforme à la politique de sécurité', { violations })
        motDePasse = c.nouveau
        simulationConnexion.definirMotDePasse(compte.identifiant, c.nouveau)
        retirer('MOT_DE_PASSE')
      }),
    enrolerTotp: () =>
      attendre((): EnrolementTotp => {
        const enrolementId = `enr-sim-${++n}`
        const secret = secretDemo()
        enrolements.set(enrolementId, { secret, expireA: Date.now() + 10 * 60_000 })
        const libelle = encodeURIComponent(`GIMAC DES:${compte.identifiant}`)
        return { enrolementId, secret, uri: `otpauth://totp/${libelle}?secret=${secret}&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1` }
      }),
    confirmerTotp: (c) =>
      attendre(() => {
        const e = enrolements.get(c.enrolementId)
        if (!e || e.expireA < Date.now()) throw refus(400, 'IAM_TOTP_ENROLMENT_EXPIRED', 'Enrôlement expiré : recommencez')
        if (!CODE.test(c.code)) throw refus(400, 'IAM_TOTP_CODE_INVALID', 'Code incorrect')
        enrolements.delete(c.enrolementId)
        compte.secondFacteur = true
        retirer('SECOND_FACTEUR')
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
  }
}
