import { ApiError } from '../http/errors'
import type { Couche } from '../http/prefixes'
import type { Session } from '../session/session'
import { ouvrirSessionDemo } from '../session/demo'
import type { ReponseConnexion, ServiceConnexion } from './modele'
import { violationsMotDePasse } from './politique'

// Connexion simulée (développement des écrans et tests de bout en bout, D-65) : mêmes états
// (CODE_REQUIS, PREMIER_ACCES, CONNECTE) et mêmes codes d'erreur que la passerelle, sur des comptes de
// démonstration par profil. Règles de la démonstration :
//   - mot de passe de tous les comptes : MOT_DE_PASSE_DEMO, sauf mot de passe propre au compte ;
//   - code : tout code de six chiffres est accepté (« 000000 » par exemple) ;
//   - 5 échecs en une minute pour un identifiant : 429 CNX_TROP_DE_TENTATIVES ;
//   - lien de réinitialisation : tout jeton base64url de 16 caractères au moins, une seule fois ;
//     « expire » (ou tout jeton plus court) : 400 CNX_JETON_INVALIDE.
// La réussite ouvre la session de démonstration de l'onglet (session/demo.ts). Aucun mot de passe n'est
// rangé dans un stockage : les changements faits pendant le premier accès vivent en mémoire de la page.

export const MOT_DE_PASSE_DEMO = 'Gimac-Demo-2026!'

export type CompteConnexionDemo = {
  identifiant: string
  /** Mot de passe propre ; MOT_DE_PASSE_DEMO à défaut. */
  motDePasse?: string
  /** Couches ouvertes : un compte d'institution n'ouvre que le portail, un compte interne que des et console. */
  couches: Couche[]
  /** ACTIF (défaut), PREMIER_ACCES (actions exigées) ou INACTIF (désactivé). */
  etat?: 'ACTIF' | 'PREMIER_ACCES' | 'INACTIF'
  /** Étapes du premier accès ; les quatre à défaut. */
  etapes?: string[]
  /** Profil de démonstration (ROLE_…) choisi par l'application après la connexion. */
  role?: string
  session: Session
}

export type OptionsConnexionSimulee = { comptes: CompteConnexionDemo[]; latenceMs?: number }

export const ETAPES_PREMIER_ACCES = ['MOT_DE_PASSE', 'SECOND_FACTEUR', 'FICHE', 'CHARTE']
const CODE = /^[0-9]{6}$/
const JETON = /^[A-Za-z0-9_-]{16,}$/
const FENETRE_MS = 60_000
const TENTATIVES_MAX = 5

// État de la démonstration partagé dans la page : mot de passe choisi et premier accès terminé
// (assistant de @gimac/compte), relus par la reconnexion de fin de premier accès.
const memoire = new Map<string, { motDePasse?: string; premierAccesTermine?: boolean }>()
const cle = (identifiant: string) => identifiant.trim().toLowerCase()

export const simulationConnexion = {
  /** Mot de passe choisi pendant la démonstration (premier accès, Mon compte). */
  definirMotDePasse(identifiant: string, motDePasse: string) {
    memoire.set(cle(identifiant), { ...memoire.get(cle(identifiant)), motDePasse })
  },
  /** Premier accès terminé : la reconnexion ouvre une session complète. */
  terminerPremierAcces(identifiant: string) {
    memoire.set(cle(identifiant), { ...memoire.get(cle(identifiant)), premierAccesTermine: true })
  },
  reinitialiser() {
    memoire.clear()
  },
}

let correlation = 0
const refus = (status: number, code: string, message: string, details?: Record<string, unknown>) =>
  new ApiError(status, code, message, `sim-cnx-${Date.now().toString(36)}-${(++correlation).toString(36)}`, details)

export function connexionSimulee(o: OptionsConnexionSimulee): ServiceConnexion {
  const latence = o.latenceMs ?? 250
  const echecs = new Map<string, number[]>()
  const jetonsUtilises = new Set<string>()
  const attendre = <T>(f: () => T) =>
    new Promise<T>((res, rej) =>
      window.setTimeout(() => {
        try {
          res(f())
        } catch (e) {
          rej(e)
        }
      }, latence),
    )
  const echouer = (identifiant: string): never => {
    const k = cle(identifiant)
    echecs.set(k, [...(echecs.get(k) ?? []), Date.now()])
    throw refus(401, 'CNX_IDENTIFIANTS_INVALIDES', 'Identifiant, mot de passe ou code incorrect.')
  }

  return {
    connecter: (d) =>
      attendre((): ReponseConnexion => {
        const k = cle(d.identifiant)
        const recents = (echecs.get(k) ?? []).filter((t) => Date.now() - t < FENETRE_MS)
        echecs.set(k, recents)
        if (recents.length >= TENTATIVES_MAX) throw refus(429, 'CNX_TROP_DE_TENTATIVES', 'Trop de tentatives : réessayez dans une minute.')
        const compte = o.comptes.find((c) => cle(c.identifiant) === k)
        const etat = memoire.get(k)
        if (!compte || d.motDePasse !== (etat?.motDePasse ?? compte.motDePasse ?? MOT_DE_PASSE_DEMO)) return echouer(d.identifiant)
        if (compte.etat === 'INACTIF') throw refus(403, 'CNX_COMPTE_INACTIF', 'Compte désactivé.')
        if (!compte.couches.includes(d.couche)) throw refus(403, 'CNX_COUCHE_REFUSEE', 'Ce compte n’ouvre pas cette application.')
        if (compte.etat === 'PREMIER_ACCES' && !etat?.premierAccesTermine) {
          const etapes = compte.etapes ?? ETAPES_PREMIER_ACCES
          ouvrirSessionDemo({ identifiant: compte.identifiant, role: compte.role, premierAcces: true, etapes })
          return { etat: 'PREMIER_ACCES', etapes }
        }
        if (d.code === undefined) return { etat: 'CODE_REQUIS' }
        if (!CODE.test(d.code)) return echouer(d.identifiant)
        echecs.delete(k)
        ouvrirSessionDemo({ identifiant: compte.identifiant, role: compte.role, premierAcces: false })
        return { etat: 'CONNECTE', session: compte.session }
      }),
    motDePasseOublie: () => attendre(() => undefined),
    nouveauMotDePasse: (jeton, motDePasse) =>
      attendre(() => {
        if (!JETON.test(jeton) || jetonsUtilises.has(jeton)) throw refus(400, 'CNX_JETON_INVALIDE', 'Lien inconnu, expiré ou déjà utilisé.')
        const violations = violationsMotDePasse(motDePasse)
        if (violations.length) throw refus(400, 'IAM_PASSWORD_POLICY', 'Mot de passe non conforme à la politique de sécurité.', { violations })
        jetonsUtilises.add(jeton)
      }),
  }
}
