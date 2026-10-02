import { useSyncExternalStore } from 'react'

// Signaux éphémères du canal poussé (D-72) : présence des comptes internes et « en train d'écrire ».
// Ils ne passent ni par le cache des requêtes ni par la reprise : un magasin en mémoire, tenu à jour par
// le canal, lu par les écrans de la messagerie.

/** Types d'événements éphémères (contrat de la passerelle et de MS-COLLABORATION). */
export const SIGNAL = {
  saisie: 'collaboration.message-typing.v1',
  message: 'collaboration.message-sent.v1',
  enLigne: 'presence.account-online.v1',
  horsLigne: 'presence.account-offline.v1',
} as const

/** Durée d'affichage d'un « écrit… » sans nouveau signal (l'émetteur en renvoie un toutes les 3 s). */
export const DUREE_SAISIE_MS = 6_000

export type EtatPresence = { enLigne: string[]; vusLe: Record<string, string> }
type Signal = { type: string; subject: string; acteur?: string | null }

let enLigne = new Set<string>()
let vusLe: Record<string, string> = {}
const saisies = new Map<string, Map<string, number>>()
const minuteries = new Map<string, number>()
let version = 0
const abonnes = new Set<() => void>()

function notifier() {
  version++
  abonnes.forEach((f) => f())
}

function abonner(f: () => void) {
  abonnes.add(f)
  return () => {
    abonnes.delete(f)
  }
}

/** État initial de la présence (GET /bff/{couche}/presence), puis tenu à jour par le canal. */
export function initialiserPresence(e: EtatPresence): void {
  enLigne = new Set(e.enLigne)
  vusLe = { ...e.vusLe }
  notifier()
}

function finSaisie(conversation: string, acteur: string) {
  const cle = `${conversation}/${acteur}`
  window.clearTimeout(minuteries.get(cle))
  minuteries.delete(cle)
  const m = saisies.get(conversation)
  if (!m?.delete(acteur)) return
  if (m.size === 0) saisies.delete(conversation)
  notifier()
}

/**
 * Applique un événement du canal : vrai s'il était éphémère (rien d'autre à faire), faux sinon. Un
 * message reçu met fin au « écrit… » de son auteur dans la conversation.
 */
export function appliquerSignal(ev: Signal): boolean {
  const acteur = ev.acteur ?? undefined
  switch (ev.type) {
    case SIGNAL.enLigne:
      if (acteur && !enLigne.has(acteur)) {
        enLigne = new Set(enLigne).add(acteur)
        notifier()
      }
      return true
    case SIGNAL.horsLigne:
      if (acteur) {
        const s = new Set(enLigne)
        s.delete(acteur)
        enLigne = s
        vusLe = { ...vusLe, [acteur]: new Date().toISOString() }
        notifier()
      }
      return true
    case SIGNAL.saisie: {
      if (!acteur) return true
      const cle = `${ev.subject}/${acteur}`
      const m = saisies.get(ev.subject) ?? new Map<string, number>()
      const nouveau = !m.has(acteur)
      m.set(acteur, Date.now())
      saisies.set(ev.subject, m)
      window.clearTimeout(minuteries.get(cle))
      minuteries.set(cle, window.setTimeout(() => finSaisie(ev.subject, acteur), DUREE_SAISIE_MS))
      if (nouveau) notifier()
      return true
    }
    case SIGNAL.message:
      if (acteur) finSaisie(ev.subject, acteur)
      return false
    default:
      return false
  }
}

/** Remise à zéro (déconnexion, tests). */
export function viderSignaux(): void {
  minuteries.forEach((t) => window.clearTimeout(t))
  minuteries.clear()
  saisies.clear()
  enLigne = new Set()
  vusLe = {}
  notifier()
}

const instantane = () => version

/** Présence d'un compte : en ligne, sinon dernière présence connue (ISO 8601). */
export function usePresence(compte: string | undefined): { enLigne: boolean; vuLe?: string } {
  useSyncExternalStore(abonner, instantane, instantane)
  if (!compte) return { enLigne: false }
  return { enLigne: enLigne.has(compte), vuLe: vusLe[compte] }
}

/** Fonction de présence, pour une liste (conversations, membres). */
export function useEnLigne(): (compte: string | undefined) => boolean {
  useSyncExternalStore(abonner, instantane, instantane)
  const courant = enLigne
  return (compte) => (compte ? courant.has(compte) : false)
}

/** Comptes qui écrivent, par conversation. */
export function useSaisies(): (conversation: string) => string[] {
  useSyncExternalStore(abonner, instantane, instantane)
  const courant = new Map(Array.from(saisies, ([c, m]) => [c, Array.from(m.keys())]))
  return (conversation) => courant.get(conversation) ?? []
}
