import type { Couche } from '../http/prefixes'
import { CONNEXION } from '../http/prefixes'
import type { Session } from './session'
import { ouvrirSessionDemo, type SessionDemo } from './demo'

// Aiguillage des comptes entre les trois applications (D-79) : un compte d'institution (jeton portant
// institution_id) n'ouvre que le portail des participants ; un compte interne ouvre l'application interne
// et, s'il porte au moins une permission d'administration de la plateforme, la console. La décision ne
// dépend que de la session (institution, permissions), jamais du nom d'un profil. La passerelle refait le
// contrôle de la couche à la connexion (CNX_COUCHE_REFUSEE) et celui de chaque permission à chaque appel.

/** Prédicat de permissions : l'une au moins de `any` (s'il est donné) et toutes celles de `all`. */
export type Predicat = { any?: readonly string[]; all?: readonly string[] }

/** Vrai si les permissions satisfont le prédicat ; un prédicat absent ou vide est toujours satisfait. */
export function satisfait(permissions: readonly string[] | undefined, p?: Predicat): boolean {
  if (!p) return true
  const tient = (x: string) => !!permissions && permissions.includes(x)
  if (p.all?.length && !p.all.every(tient)) return false
  if (p.any?.length && !p.any.some(tient)) return false
  return true
}

/** Permissions qui ouvrent la console d'administration de la plateforme (l'une suffit). */
export const PERMISSIONS_CONSOLE = [
  'IAM_ACCOUNT_READ', 'PLATFORM_SUPERVISION_READ', 'PLATFORM_CONFIG_READ', 'PLATFORM_OPS_READ', 'PLATFORM_SECURITY_POLICY_READ', 'PLATFORM_MESSAGING_EDIT',
] as const

const sansBarreFinale = (u: unknown, defaut: string) => String(u || defaut).replace(/\/+$/, '')

/**
 * Adresses publiques des trois applications : VITE_URL_DES (ou VITE_URL_DEPARTEMENT), VITE_URL_PORTAIL et
 * VITE_URL_CONSOLE, fixées à la construction (arguments URL_DES, URL_PORTAIL et URL_CONSOLE de l'image).
 */
export const ADRESSES_APPLICATIONS: Record<Couche, string> = {
  des: sansBarreFinale(import.meta.env.VITE_URL_DES || import.meta.env.VITE_URL_DEPARTEMENT, 'http://localhost:4200'),
  portail: sansBarreFinale(import.meta.env.VITE_URL_PORTAIL, 'http://localhost:4201'),
  console: sansBarreFinale(import.meta.env.VITE_URL_CONSOLE, 'http://localhost:4202'),
}

/** Compte d'institution (correspondant d'un participant) : la session porte une institution. */
export function estCompteInstitution(s: Pick<Session, 'institutionId'>): boolean {
  return !!s.institutionId?.trim()
}

/** Vrai si les permissions ouvrent la console (au moins une permission d'administration de la plateforme). */
export function ouvreConsole(permissions: readonly string[] | undefined): boolean {
  return satisfait(permissions, { any: PERMISSIONS_CONSOLE })
}

/**
 * Application où la session doit travailler quand elle ouvre l'application `couche` : la même si elle y a
 * sa place, sinon celle vers laquelle la renvoyer. Pendant le premier accès (session restreinte, aucune
 * permission encore), un compte interne reste là où il s'est connecté : l'assistant s'y déroule.
 */
export function applicationDe(s: Pick<Session, 'institutionId' | 'permissions' | 'restreinte'>, couche: Couche): Couche {
  if (estCompteInstitution(s)) return 'portail'
  if (couche === 'portail') return 'des'
  if (couche === 'console' && !s.restreinte && !ouvreConsole(s.permissions)) return 'des'
  return couche
}

/**
 * Application à proposer après un refus de couche de la passerelle (CNX_COUCHE_REFUSEE) : le portail refuse
 * les comptes internes (application interne) ; l'application interne et la console refusent les comptes
 * d'institution (portail des participants).
 */
export function applicationApresRefus(couche: Couche): Couche {
  return couche === 'portail' ? 'des' : 'portail'
}

/** Fragment d'adresse portant la session de démonstration d'une application à l'autre (services simulés). */
export const FRAGMENT_SESSION_DEMO = 'gm-session-demo'

/**
 * Adresse d'une page d'une autre application (tableau de bord par défaut). En démonstration (services
 * simulés), la session de démonstration voyage dans le fragment, jamais envoyé au serveur : le stockage
 * de l'onglet est propre à chaque origine.
 */
export function adresseApplication(couche: Couche, chemin = `/${couche}/tableau-de-bord`, demo?: SessionDemo | null): string {
  const base = `${ADRESSES_APPLICATIONS[couche]}${chemin}`
  return demo ? `${base}#${FRAGMENT_SESSION_DEMO}=${encodeURIComponent(JSON.stringify(demo))}` : base
}

/** Écran de connexion d'une autre application, identifiant prérempli et motif de l'aiguillage. */
export function adresseConnexionApplication(couche: Couche, o: { identifiant?: string; depuis: Couche }): string {
  const q = new URLSearchParams({ motif: 'aiguillage', depuis: o.depuis })
  if (o.identifiant?.trim()) q.set('identifiant', o.identifiant.trim())
  return `${ADRESSES_APPLICATIONS[couche]}${CONNEXION}?${q.toString()}`
}

/**
 * Reprend la session de démonstration apportée par le fragment d'adresse (services simulés seulement) :
 * l'ouvre dans l'onglet et retire le fragment. Vrai si une session a été reprise.
 */
export function reprendreSessionDemo(): boolean {
  if (typeof window === 'undefined') return false
  const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''))
  const brut = fragment.get(FRAGMENT_SESSION_DEMO)
  if (!brut) return false
  try {
    const s = JSON.parse(brut) as SessionDemo
    if (!s || typeof s.identifiant !== 'string') return false
    ouvrirSessionDemo(s)
    return true
  } catch {
    return false
  } finally {
    window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search)
  }
}
