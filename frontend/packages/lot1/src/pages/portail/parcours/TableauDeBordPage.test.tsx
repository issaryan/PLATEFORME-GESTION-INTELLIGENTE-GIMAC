import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TableauDeBordPage from './TableauDeBordPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './outilsTest'

afterEach(cleanup)

const CHEMIN = '/portail/tableau-de-bord'
const monter = (o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<TableauDeBordPage />, CHEMIN, CHEMIN, o)

describe('Portail : tableau de bord du participant', () => {
  it('compteurs du service, tâches menant à leur geste, liaisons cloisonnées à l’institution du jeton', async () => {
    const { container } = monter()
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon espace de certification' })).toBeTruthy()
    expect(await screen.findByText('Procès-verbaux à signer')).toBeTruthy()
    for (const c of ['Tâches à réaliser', 'Attestations à fournir', 'Fiches de suivi à renseigner', 'Tâches en retard', 'Échéances proches']) expect(screen.getByText(c)).toBeTruthy()
    const taches = screen.getAllByRole('table')[0]
    expect(await within(taches).findByRole('button', { name: 'Signer' })).toBeTruthy()
    expect(within(taches).getByRole('button', { name: 'Communiquer' })).toBeTruthy()
    expect(within(taches).getByRole('button', { name: 'Renseigner' })).toBeTruthy()
    expect(within(taches).getByText('Signature du procès-verbal')).toBeTruthy()
    const liaisons = screen.getAllByRole('table')[1]
    expect(await within(liaisons).findByText('LIA-2026-030')).toBeTruthy()
    expect(within(liaisons).getByText('Bloquée')).toBeTruthy()
    expect(document.body.textContent).not.toMatch(/LIA-2026-022|LIA-2026-041|Banque G/)
    sansCleNiNomInterne()
    await auditer(container)
  })

  it('le geste suit la permission du jeton : sans HOM_PORTAL_SUBMIT, la tâche reste visible sans action', async () => {
    monter({ sans: ['HOM_PORTAL_SUBMIT'] })
    const taches = (await screen.findAllByRole('table'))[0]
    expect(await within(taches).findByRole('button', { name: 'Signer' })).toBeTruthy()
    expect(within(taches).queryByRole('button', { name: 'Communiquer' })).toBeNull()
    expect(within(taches).getAllByText('Au tour du GIMAC').length).toBeGreaterThan(0)
  })

  it('aucune tâche : état vide explicite', async () => {
    const s = services((b) => ({ mesTaches: async () => ({ ...(await b.portailParcours.mesTaches()), taches: [] }) }))
    monter({ services: s })
    expect(await screen.findByText('Rien ne vous est demandé pour le moment')).toBeTruthy()
  })

  it('refus du service : erreur avec identifiant de corrélation et réessai', async () => {
    const s = services(() => ({ mesTaches: () => Promise.reject(new ApiError(503, 'HOM_UNAVAILABLE', 'Service indisponible.', 'cid-tdb')) }))
    monter({ services: s })
    const alertes = await screen.findAllByRole('alert')
    expect(alertes.some((a) => a.textContent?.includes('cid-tdb'))).toBe(true)
    expect(screen.getAllByRole('button', { name: 'Réessayer' }).length).toBeGreaterThan(0)
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter({ langue })
    await screen.findByText(langue === 'en' ? 'My certification space' : 'Mi espacio de certificación')
    await screen.findByText(langue === 'en' ? 'Sign' : 'Firmar')
    sansCleNiNomInterne()
  })
})
