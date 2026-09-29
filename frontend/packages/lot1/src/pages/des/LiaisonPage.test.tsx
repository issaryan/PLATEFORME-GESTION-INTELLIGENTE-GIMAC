import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import LiaisonPage from './LiaisonPage'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

afterEach(cleanup)
const cleBrute = /\b(liaison|operation|couverture|graphe|perimetre|preuves|campagne|scenario|resultats|partie|segment|commun|crud)\.[a-zA-Z]/
const avec = (surcharge: Partial<Services['homologation']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, homologation: { ...s.homologation, ...surcharge } }
}
const page = (id: string, o: { onglet?: string; services?: Services; langue?: 'fr' | 'en' | 'es' } = {}) =>
  rendrePage(<LiaisonPage />, { chemin: `/des/liaisons/${id}${o.onglet ? `?onglet=${o.onglet}` : ''}`, motif: '/des/liaisons/:id', langue: o.langue, services: o.services })

describe('DES-09 Vue d\'une liaison', () => {
  it('nominal : fiche de la liaison, statut et onglets', async () => {
    page('LIA-2026-017')
    expect((await screen.findByRole('heading', { level: 1 })).textContent).toBe('Établissement de paiement A')
    for (const o of ['Graphe', 'Périmètre', 'Opérations', 'Résultats du simulateur', 'Preuves']) expect(screen.getByRole('tab', { name: o })).toBeTruthy()
  })
  it('chargement : squelettes', () => {
    const { container } = page('LIA-2026-017', { services: avec({ lireLiaison: () => new Promise(() => undefined) }) })
    expect(container.querySelectorAll('.gm-skeleton').length).toBeGreaterThan(0)
  })
  it('erreur : liaison introuvable avec identifiant de corrélation', async () => {
    page('LIA-0000-000', { services: avec({ lireLiaison: async () => { throw new ApiError(404, 'HOM_LIAISON_NOT_FOUND', 'Liaison introuvable.', 'RQ-2026-0929-44') } }) })
    expect(await screen.findByText('RQ-2026-0929-44')).toBeTruthy()
  })
  it('onglet graphe : les opérations du service, par couche', async () => {
    page('LIA-2026-017', { onglet: 'graphe' })
    expect(await screen.findByText('OP-E01')).toBeTruthy()
    expect(screen.getByText(/Couche 1/)).toBeTruthy()
  })
  it('onglet graphe vide : état vide', async () => {
    const { container } = page('LIA-2026-017', { onglet: 'graphe', services: avec({ grapheLiaison: async () => ({ noeuds: [], aretes: [], couches: [] }) }) })
    await screen.findByText('Aucune opération instanciée pour cette liaison')
    expect(container.querySelector('.gm-empty')).toBeTruthy()
  })
  it('onglet résultats : rappel de lecture seule et scénario inconnu à qualifier', async () => {
    page('LIA-2026-017', { onglet: 'resultats' })
    expect(await screen.findByText('Résultats repris du simulateur, jamais recalculés.')).toBeTruthy()
    expect(await screen.findByText('SC-XYZ-99')).toBeTruthy()
  })
  it('le dégel n\'est proposé que si le service le permet', async () => {
    page('LIA-2026-021')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('button', { name: 'Dégeler la liaison' })).toBeTruthy()
    cleanup()
    page('LIA-2026-017')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('button', { name: 'Dégeler la liaison' })).toBeNull()
  })
  it('mène au dossier de mise en service pour une liaison apte', async () => {
    page('LIA-2025-102')
    await screen.findByRole('heading', { level: 1 })
    expect(screen.getByRole('button', { name: 'Ouvrir le dossier de mise en service' })).toBeTruthy()
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page('LIA-2026-017', { onglet: 'perimetre', langue })
    await screen.findByRole('heading', { level: 1 })
    await screen.findAllByText(/Retrait/)
    expect(container.textContent).not.toMatch(cleBrute)
  })
})
