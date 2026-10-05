import type { QueryClient } from '@tanstack/react-query'
import type { Couche } from '../http/prefixes'
import { appliquerSignal } from './signaux'

// Canal poussé (F0 §A3) : un flux d'événements par couche, repris depuis le dernier identifiant reçu ;
// chaque événement invalide les requêtes dont la clé commence par son sujet ([sujet, ressource, id]).
// Signaux éphémères (présence, « écrit… », D-72) : appliqués au magasin des signaux, sans invalidation
// ni identifiant de reprise ; à chaque (re)connexion, l'état de la présence (clé ['presence']) est relu.
// Notifications de la plateforme (D-80) : sujet « notification », remis au seul destinataire, à la création
// d'une notification et à chaque regroupement ; il invalide la cloche, la liste et le compteur (clés ['notification', …]).
export type LiveTopic = 'homologation' | 'certification' | 'collaboration' | 'presence' | 'notification'
export type LiveEvent = { id: string; type: string; subject: string; topic: LiveTopic; acteur?: string | null }
export type EtatCanal = 'connected' | 'lost'
const ALIAS: Partial<Record<LiveTopic, string[]>> = { homologation: ['parcours'], certification: ['parcours'] }

export function openLiveChannel(
  layer: Couche,
  topics: LiveTopic[],
  qc: QueryClient,
  onState: (s: EtatCanal) => void,
  backoff: { baseMs: number; maxMs: number },
): () => void {
  let source: EventSource | null = null
  let attempt = 0
  let lastId = ''
  let closed = false
  let minuterie: number | undefined
  const connect = () => {
    const q = new URLSearchParams({ sujets: topics.join(',') })
    if (lastId) q.set('depuis', lastId)
    source = new EventSource(`/bff/${layer}/live?${q.toString()}`, { withCredentials: true })
    source.onopen = () => {
      attempt = 0
      onState('connected')
      if (topics.includes('presence')) void qc.invalidateQueries({ queryKey: ['presence'] })
    }
    source.onmessage = (m) => {
      const ev = JSON.parse(m.data as string) as LiveEvent
      if (appliquerSignal(ev)) return
      lastId = ev.id
      void qc.invalidateQueries({ queryKey: [ev.topic] })
      // Le parcours de la liaison (étapes 3 à 10) se lit sous le sujet « parcours » : un événement
      // d'homologation ou de certification le rafraîchit aussi.
      ;(ALIAS[ev.topic] ?? []).forEach((cle) => void qc.invalidateQueries({ queryKey: [cle] }))
    }
    source.onerror = () => {
      source?.close()
      onState('lost')
      if (closed) return
      minuterie = window.setTimeout(connect, Math.min(backoff.maxMs, 2 ** attempt++ * backoff.baseMs))
    }
  }
  connect()
  return () => {
    closed = true
    window.clearTimeout(minuterie)
    source?.close()
  }
}
