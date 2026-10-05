import { ETAPES_PREMIER_ACCES, MOT_DE_PASSE_DEMO, ReauthCancelled, avatarsSimules, simulationConnexion, violationsMotDePasse, type CleSecurite } from '@gimac/socle'
import { LANGUES_COMPTE, PHOTO_PROFIL, type Appareil, type Compte, type Connexion, type DescriptionAppareil, type EnrolementTotp, type Moi, type RattachementCompte, type ResultatConnexion, type SessionCompte } from '../modele'
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
  /** Couche de la session courante (appareils connectés) ; « des » à défaut. */
  couche?: string
  /** Photo de profil de démonstration déjà déposée. */
  photo?: boolean
  /** Rattachement organisationnel de démonstration (D-76) : servi par organisation(), couche interne seulement. */
  rattachement?: RattachementCompte
}

const TELEPHONE = /^\+?[0-9 ]{8,20}$/
const CODE = /^[0-9]{6}$/
const BASE32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
const il = (heures: number) => new Date(Date.now() - heures * 3_600_000).toISOString()

const minutes = (m: number) => new Date(Date.now() - m * 60_000).toISOString()

/** Appareils connectés de démonstration : la session courante, puis des sessions plus anciennes (IP masquées). */
export function appareilsDemo(couche = 'des'): Appareil[] {
  const a = (navigateur: string, systeme: string, type: DescriptionAppareil['type']): DescriptionAppareil => ({ navigateur, systeme, type })
  return [
    { id: 'app-7f3c2a', courante: true, appareil: a('Chrome 129', 'Windows 11', 'ORDINATEUR'), adresseIp: '10.20.1.15', couche, ouverteLe: minutes(64), derniereActivite: minutes(0), expireA: new Date(Date.now() + 30 * 60_000).toISOString() },
    { id: 'app-91b4e0', courante: false, appareil: a('Firefox 131', 'Ubuntu 24.04', 'ORDINATEUR'), adresseIp: '10.20.4.•', couche, ouverteLe: minutes(60 * 26), derniereActivite: minutes(60 * 20 + 12), expireA: minutes(-10) },
    { id: 'app-2d8e57', courante: false, appareil: a('Safari 18', 'iOS 18', 'MOBILE'), adresseIp: '41.202.219.•', couche: 'des', ouverteLe: minutes(60 * 3 + 5), derniereActivite: minutes(42), expireA: minutes(-12) },
    { id: 'app-c05a19', courante: false, appareil: a('Chrome 129', 'Android 14', 'TABLETTE'), adresseIp: '10.20.7.•', couche: 'portail', ouverteLe: minutes(60 * 50), derniereActivite: minutes(60 * 49), expireA: minutes(-60 * 48) },
    { id: 'app-e6f310', courante: false, appareil: a('Edge 129', 'Windows 10', 'ORDINATEUR'), adresseIp: '10.20.2.•', couche, ouverteLe: minutes(60 * 6), derniereActivite: minutes(95), expireA: minutes(-20) },
  ]
}

/** Historique de démonstration : 90 jours de connexions, la plus récente d'abord, échecs et un blocage. */
export function historiqueDemo(couche = 'des'): Connexion[] {
  const postes: [string, string, DescriptionAppareil['type'], string][] = [
    ['Chrome 129', 'Windows 11', 'ORDINATEUR', '10.20.1.15'],
    ['Firefox 131', 'Ubuntu 24.04', 'ORDINATEUR', '10.20.4.•'],
    ['Safari 18', 'iOS 18', 'MOBILE', '41.202.219.•'],
    ['Edge 129', 'Windows 10', 'ORDINATEUR', '10.20.2.•'],
  ]
  const lignes: Connexion[] = []
  for (let i = 0; i < 64; i++) {
    const [navigateur, systeme, type, ip] = postes[(i * 7) % postes.length]
    const resultat: ResultatConnexion = i === 23 ? 'BLOQUEE' : i % 11 === 4 ? 'ECHEC_MOT_DE_PASSE' : i % 13 === 7 ? 'ECHEC_CODE' : 'REUSSIE'
    lignes.push({ le: minutes(64 + i * 60 * 33 + (i % 5) * 17), resultat, appareil: { navigateur, systeme, type }, adresseIp: ip.replace(/\.\d+$/, '.•'), couche: i % 9 === 5 ? 'portail' : couche })
  }
  return lignes
}

/** Lit une image (Blob) en adresse data: ; rien si illisible. */
function lireEnAdresse(b: Blob): Promise<string | null> {
  return new Promise((res) => {
    const r = new FileReader()
    r.onload = () => res(typeof r.result === 'string' ? r.result : null)
    r.onerror = () => res(null)
    r.readAsDataURL(b)
  })
}

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
  if (o.photo) compte.avatarVersion = avatarsSimules.demo(compte.id)
  else compte.avatarVersion ??= avatarsSimules.version(compte.id)
  let appareils = appareilsDemo(o.couche)
  const historique = historiqueDemo(o.couche)
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
        if (c.fermerAutresSessions) appareils = appareils.filter((a) => a.courante)
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
    // Photo de profil : mêmes refus que MS-IAM (type réel, poids) ; image gardée en mémoire de la page.
    changerPhoto: async (image) => {
      const adresse = PHOTO_PROFIL.types.includes(image.type) && image.size > 0 && image.size <= PHOTO_PROFIL.tailleMaxOctets ? await lireEnAdresse(image) : null
      return attendre(() => {
        if (!PHOTO_PROFIL.types.includes(image.type)) throw refus(400, 'IAM_AVATAR_TYPE', 'Format d’image non accepté')
        if (image.size > PHOTO_PROFIL.tailleMaxOctets) throw refus(400, 'IAM_AVATAR_TOO_LARGE', 'Image trop volumineuse')
        if (!adresse || image.size === 0) throw refus(400, 'IAM_AVATAR_UNREADABLE', 'Image illisible')
        compte.avatarVersion = avatarsSimules.definir(compte.id, adresse)
        return { version: compte.avatarVersion }
      })
    },
    retirerPhoto: () =>
      attendre(() => {
        avatarsSimules.retirer(compte.id)
        compte.avatarVersion = null
      }),
    appareils: () => attendre(() => appareils),
    fermerAppareil: (id) =>
      attendre(() => {
        const a = appareils.find((x) => x.id === id)
        if (!a) throw refus(404, 'CNX_APPAREIL_INCONNU', 'Session inconnue')
        if (a.courante) throw refus(409, 'CNX_SESSION_COURANTE', 'Utilisez la déconnexion pour fermer la session courante')
        appareils = appareils.filter((x) => x.id !== id)
      }),
    fermerAutresAppareils: () =>
      attendre(() => {
        appareils = appareils.filter((x) => x.courante)
      }),
    historique: (page, taille) => attendre(() => ({ contenu: historique.slice(page * taille, (page + 1) * taille), total: historique.length })),
    ...(o.rattachement ? { organisation: () => attendre(() => o.rattachement!) } : {}),
  }
}
