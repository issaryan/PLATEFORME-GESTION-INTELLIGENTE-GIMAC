import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, render, screen } from '@testing-library/react'
import { DUREE_SAISIE_MS, SIGNAL, appliquerSignal, initialiserPresence, useEnLigne, usePresence, useSaisies, viderSignaux } from './signaux'

// Magasin des signaux éphémères (D-72) : présence initiale puis tenue à jour, « écrit… » expirant de
// lui-même ou à l'arrivée du message de son auteur.
function Vue({ compte, conversation }: { compte: string; conversation: string }) {
  const p = usePresence(compte)
  const enLigne = useEnLigne()
  const saisies = useSaisies()
  return (
    <p>
      <span>{`${compte}:${p.enLigne ? 'en ligne' : `vu ${p.vuLe ?? 'jamais'}`}`}</span>
      <span>{`liste:${enLigne(compte)}`}</span>
      <span>{`ecrit:${saisies(conversation).join(',') || 'personne'}`}</span>
    </p>
  )
}

beforeEach(() => {
  vi.useFakeTimers()
  viderSignaux()
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
})

describe('signaux éphémères', () => {
  it('présence : état initial, arrivée, départ avec dernière présence', () => {
    render(<Vue compte="u2" conversation="C1" />)
    expect(screen.getByText('u2:vu jamais')).toBeTruthy()
    act(() => initialiserPresence({ enLigne: ['u2'], vusLe: {} }))
    expect(screen.getByText('u2:en ligne')).toBeTruthy()
    expect(screen.getByText('liste:true')).toBeTruthy()
    act(() => void appliquerSignal({ type: SIGNAL.horsLigne, subject: 'u2', acteur: 'u2' }))
    expect(screen.getByText(/^u2:vu \d{4}-/)).toBeTruthy()
    act(() => void appliquerSignal({ type: SIGNAL.enLigne, subject: 'u2', acteur: 'u2' }))
    expect(screen.getByText('u2:en ligne')).toBeTruthy()
    expect(appliquerSignal({ type: SIGNAL.enLigne, subject: 'u2', acteur: 'u2' })).toBe(true)
    expect(appliquerSignal({ type: SIGNAL.horsLigne, subject: 'x' })).toBe(true)
  })
  it('« écrit… » : affiché, prolongé par chaque signal, expiré seul, effacé par le message de son auteur', () => {
    render(<Vue compte="u2" conversation="C1" />)
    act(() => void appliquerSignal({ type: SIGNAL.saisie, subject: 'C1', acteur: 'u2' }))
    act(() => void appliquerSignal({ type: SIGNAL.saisie, subject: 'C1', acteur: 'u3' }))
    expect(screen.getByText('ecrit:u2,u3')).toBeTruthy()
    act(() => vi.advanceTimersByTime(DUREE_SAISIE_MS - 1000))
    act(() => void appliquerSignal({ type: SIGNAL.saisie, subject: 'C1', acteur: 'u2' }))
    act(() => vi.advanceTimersByTime(1000))
    expect(screen.getByText('ecrit:u2')).toBeTruthy()
    let ephemere = true
    act(() => {
      ephemere = appliquerSignal({ type: SIGNAL.message, subject: 'C1', acteur: 'u2' })
    })
    expect(ephemere).toBe(false)
    expect(screen.getByText('ecrit:personne')).toBeTruthy()
    expect(appliquerSignal({ type: SIGNAL.saisie, subject: 'C1' })).toBe(true)
    expect(appliquerSignal({ type: 'homologation.liaison-updated.v1', subject: 'L1' })).toBe(false)
    expect(appliquerSignal({ type: SIGNAL.message, subject: 'C9', acteur: 'u9' })).toBe(false)
  })
})
