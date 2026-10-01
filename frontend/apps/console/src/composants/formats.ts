import type { LiveState } from '@gimac/design-system'
import type { ApiError } from '@gimac/socle'
import type { EtatFlux } from '../modele/plateforme'

// Outils de présentation de la console : grille des indicateurs, état des listes, formats.


/** État d'une requête de liste pour DataTable. */
export function etatListe(q: { isPending: boolean; isError: boolean; error: ApiError | null }, lignes: unknown[] | undefined): { state: 'loading' | 'error' | 'empty' | 'ok'; error?: { message: string; correlationId?: string } } {
  if (q.isPending) return { state: 'loading' }
  if (q.isError) return { state: 'error', error: { message: q.error?.message ?? '', correlationId: q.error?.correlationId || undefined } }
  return { state: lignes && lignes.length ? 'ok' : 'empty' }
}

/** État du flux de la console vers l'état du composant LiveStatGroup. */
export const etatDirect = (e: EtatFlux): LiveState => (e === 'connecte' ? 'live' : e === 'perdu' ? 'lost' : 'connecting')

// ------------------------------------------------------------------ formats

export const pourcent = (v: number | null | undefined, echelle = 1) => (v === null || v === undefined ? '—' : `${(v * echelle).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %`)
export const nombre = (v: number | null | undefined, decimales: number) => (v === null || v === undefined ? '—' : v.toLocaleString('fr-FR', { maximumFractionDigits: decimales }))
export function octets(v: number | null | undefined): string {
  if (v === null || v === undefined) return '—'
  const u = ['o', 'Ko', 'Mo', 'Go', 'To']
  let x = v
  let i = 0
  while (x >= 1024 && i < u.length - 1) {
    x /= 1024
    i++
  }
  return `${x.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} ${u[i]}`
}
