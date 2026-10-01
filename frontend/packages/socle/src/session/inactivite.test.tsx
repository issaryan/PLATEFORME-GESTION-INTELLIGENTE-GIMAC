import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { ApiError } from '../http/errors'
import { initI18n } from '../i18n/i18n'
import { navigation } from './deconnexion'
import { CANAL_SESSION, INACTIVITE_MAX_MS, SurveillanceInactivite, dureeInactivite, minutesSecondes, prolongerSession } from './inactivite'

// D-65 §6 : fermeture après 30 minutes d'inactivité, avertissement à 28, activité partagée entre onglets.

// Canal entre onglets simulé : livraison synchrone aux autres instances du même nom.
class CanalFictif {
  static instances: CanalFictif[] = []
  onmessage: ((m: MessageEvent) => void) | null = null
  envoyes: unknown[] = []
  readonly name: string
  constructor(name: string) {
    this.name = name
    CanalFictif.instances.push(this)
  }
  postMessage(data: unknown) {
    this.envoyes.push(data)
    CanalFictif.instances.filter((c) => c !== this && c.name === this.name).forEach((c) => c.onmessage?.({ data } as MessageEvent))
  }
  close() {
    CanalFictif.instances = CanalFictif.instances.filter((c) => c !== this)
  }
}

beforeAll(() => {
  initI18n([], 'fr')
})
beforeEach(() => {
  vi.useFakeTimers()
  CanalFictif.instances = []
  vi.stubGlobal('BroadcastChannel', CanalFictif)
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

const avancer = (ms: number) => act(() => { vi.advanceTimersByTime(ms) })
const MIN = 60_000

describe('durée et compte à rebours', () => {
  it('durée lue sur expireA, bornée entre 1 et 30 minutes ; 30 minutes à défaut', () => {
    const t0 = Date.parse('2026-09-30T10:00:00Z')
    expect(dureeInactivite(undefined, t0)).toBe(INACTIVITE_MAX_MS)
    expect(dureeInactivite('pas une date', t0)).toBe(INACTIVITE_MAX_MS)
    expect(dureeInactivite('2026-09-30T10:14:59.800Z', t0)).toBe(15 * MIN)
    expect(dureeInactivite('2026-09-30T12:00:00Z', t0)).toBe(30 * MIN)
    expect(dureeInactivite('2026-09-30T09:00:00Z', t0)).toBe(MIN)
  })
  it('mm:ss', () => {
    expect(minutesSecondes(120_000)).toBe('02:00')
    expect(minutesSecondes(61_500)).toBe('01:02')
    expect(minutesSecondes(-5)).toBe('00:00')
  })
  it('prolongation : POST /api/session/prolongation', async () => {
    vi.useRealTimers()
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(null, { status: 204 }))
    await prolongerSession()
    expect(String(spy.mock.calls[0][0])).toContain('/api/session/prolongation')
    expect(spy.mock.calls[0][1]?.method).toBe('POST')
  })
})

describe('SurveillanceInactivite', () => {
  it('avertissement à 28 minutes avec compte à rebours, puis fermeture à 30 minutes', () => {
    const expirer = vi.fn()
    render(<SurveillanceInactivite prolonger={vi.fn(async () => undefined)} expirer={expirer} />)
    avancer(27 * MIN)
    expect(screen.queryByRole('alertdialog')).toBeNull()
    avancer(MIN + 1000)
    expect(screen.getByRole('alertdialog', { name: 'Votre session va expirer' })).toBeTruthy()
    expect(screen.getByTestId('compte-a-rebours').textContent).toBe('01:59')
    // Pendant l'avertissement, bouger la souris ne suffit pas.
    fireEvent.mouseMove(window)
    avancer(2 * MIN)
    expect(expirer).toHaveBeenCalledWith(30)
    expect(CanalFictif.instances[0].envoyes).toContainEqual({ type: 'expiration' })
  })

  it('activité (souris, clavier, défilement, toucher) : minuterie remise à zéro et partagée', () => {
    const expirer = vi.fn()
    render(<SurveillanceInactivite prolonger={vi.fn(async () => undefined)} expirer={expirer} />)
    avancer(20 * MIN)
    fireEvent.keyDown(window, { key: 'a' })
    avancer(20 * MIN)
    fireEvent.scroll(window)
    avancer(20 * MIN)
    fireEvent.touchStart(window)
    avancer(20 * MIN)
    expect(expirer).not.toHaveBeenCalled()
    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(CanalFictif.instances[0].envoyes.filter((m) => (m as { type: string }).type === 'activite').length).toBeGreaterThanOrEqual(3)
  })

  it('activité : la session serveur est prolongée au plus toutes les 5 minutes', () => {
    const prolonger = vi.fn(async () => undefined)
    render(<SurveillanceInactivite prolonger={prolonger} expirer={vi.fn()} />)
    avancer(MIN)
    fireEvent.mouseDown(window)
    expect(prolonger).not.toHaveBeenCalled()
    avancer(5 * MIN)
    fireEvent.mouseDown(window)
    fireEvent.mouseDown(window)
    expect(prolonger).toHaveBeenCalledTimes(1)
  })

  it('« Rester connecté » : prolongation, fenêtre fermée, autres onglets prévenus', async () => {
    const prolonger = vi.fn(async () => undefined)
    const expirer = vi.fn()
    render(<SurveillanceInactivite prolonger={prolonger} expirer={expirer} />)
    avancer(29 * MIN)
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Rester connecté' }))
    })
    expect(prolonger).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(CanalFictif.instances[0].envoyes).toContainEqual(expect.objectContaining({ type: 'prolongation' }))
    avancer(27 * MIN)
    expect(expirer).not.toHaveBeenCalled()
  })

  it('« Rester connecté » refusé (401) : fermeture ; autre échec : l’avertissement reste', async () => {
    const expirer = vi.fn()
    const prolonger = vi.fn().mockRejectedValueOnce(new ApiError(503, 'HTTP_503', '', '')).mockRejectedValueOnce(new ApiError(401, 'CNX_SESSION_EXPIREE', '', ''))
    render(<SurveillanceInactivite prolonger={prolonger} expirer={expirer} />)
    avancer(29 * MIN)
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Rester connecté' }))
    })
    expect(screen.getByRole('alertdialog')).toBeTruthy()
    expect(expirer).not.toHaveBeenCalled()
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))
    })
    expect(expirer).toHaveBeenCalledTimes(1)
  })

  it('« Se déconnecter » depuis l’avertissement', () => {
    const expirer = vi.fn()
    render(<SurveillanceInactivite prolonger={vi.fn(async () => undefined)} expirer={expirer} />)
    avancer(28 * MIN + 1000)
    fireEvent.click(screen.getByRole('button', { name: 'Se déconnecter' }))
    expect(expirer).toHaveBeenCalledTimes(1)
  })

  it('autres onglets : l’activité ou la prolongation d’un onglet ferme l’avertissement ; l’expiration ferme tout', () => {
    const expirer = vi.fn()
    render(<SurveillanceInactivite prolonger={vi.fn(async () => undefined)} expirer={expirer} />)
    const autre = new CanalFictif(CANAL_SESSION)
    avancer(29 * MIN)
    expect(screen.getByRole('alertdialog')).toBeTruthy()
    act(() => autre.postMessage({ type: 'activite', t: Date.now() }))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    avancer(29 * MIN)
    act(() => autre.postMessage({ type: 'prolongation' }))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    act(() => autre.postMessage({ type: 'expiration' }))
    expect(expirer).toHaveBeenCalledTimes(1)
    act(() => autre.postMessage({ type: 'expiration' }))
    avancer(31 * MIN)
    expect(expirer).toHaveBeenCalledTimes(1)
  })

  it('durée de la session (expireA) : 10 minutes, avertissement à 8', () => {
    const expirer = vi.fn()
    render(<SurveillanceInactivite expireA={new Date(Date.now() + 10 * MIN).toISOString()} prolonger={vi.fn(async () => undefined)} expirer={expirer} />)
    avancer(8 * MIN + 1000)
    expect(screen.getByRole('alertdialog')).toBeTruthy()
    avancer(2 * MIN)
    expect(expirer).toHaveBeenCalledWith(10)
  })

  it('par défaut : déconnexion et retour à la connexion avec le motif « inactivité » ; sans BroadcastChannel', async () => {
    vi.stubGlobal('BroadcastChannel', undefined)
    const vers = vi.spyOn(navigation, 'vers').mockImplementation(() => undefined)
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    render(<SurveillanceInactivite />)
    await act(async () => {
      vi.advanceTimersByTime(30 * MIN + 1000)
    })
    await act(async () => {
      await vi.runOnlyPendingTimersAsync()
    })
    expect(vers).toHaveBeenCalledWith(expect.stringContaining('/connexion?motif=inactivite&minutes=30'))
  })
})
