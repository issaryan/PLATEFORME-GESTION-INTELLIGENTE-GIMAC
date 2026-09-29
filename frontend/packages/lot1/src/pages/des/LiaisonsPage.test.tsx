import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, waitFor } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import LiaisonsPage from './LiaisonsPage'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

afterEach(cleanup)
const cleBrute = /\b(liaisons?|liaison|operation|couverture|pilotage|commun|crud)\.[a-zA-Z]/
const avec = (surcharge: Partial<Services['homologation']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, homologation: { ...s.homologation, ...surcharge } }
}
const page = (o: { services?: Services; langue?: 'fr' | 'en' | 'es'; chemin?: string } = {}) =>
  rendrePage(<LiaisonsPage />, { chemin: o.chemin ?? '/des/liaisons', motif: '/des/liaisons', langue: o.langue, services: o.services })

describe('DES-08 Liaisons et parcours', () => {
  it('nominal : liste les liaisons et leurs institutions membres', async () => {
    page()
    expect(await screen.findByText('Établissement de paiement A')).toBeTruthy()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Liaisons et parcours')
    expect(screen.getByText('Banque D')).toBeTruthy()
  })
  it('chargement : squelettes pendant la requête', () => {
    const { container } = page({ services: avec({ listerLiaisons: () => new Promise(() => undefined) }) })
    expect(container.querySelectorAll('.gm-skeleton').length).toBeGreaterThan(0)
  })
  it('vide : état vide avec sa cause', async () => {
    const { container } = page({ services: avec({ listerLiaisons: async () => ({ contenu: [], total: 0 }) }) })
    await waitFor(() => expect(container.querySelector('.gm-empty')).toBeTruthy())
  })
  it('erreur : identifiant de corrélation et réessai', async () => {
    page({ services: avec({ listerLiaisons: async () => { throw new ApiError(503, 'SERVICE_INDISPONIBLE', 'Service indisponible.', 'RQ-2026-0929-7') } }) })
    expect(await screen.findByText('RQ-2026-0929-7')).toBeTruthy()
    expect(screen.getAllByRole('button').some((b) => /R[ée]essayer|Retry|Reintentar/.test(b.textContent ?? ''))).toBe(true)
  })
  it('ne propose le dégel que pour la liaison dont le service le permet', async () => {
    page()
    await screen.findByText('Banque D')
    expect(screen.getAllByRole('button', { name: 'Dégeler la liaison' })).toHaveLength(1)
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page({ langue })
    await screen.findByText('Établissement de paiement A')
    expect(container.textContent).not.toMatch(cleBrute)
    expect(container.textContent).not.toContain('Liaisons et parcours')
  })
})
