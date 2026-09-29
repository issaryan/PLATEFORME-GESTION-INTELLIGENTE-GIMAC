import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import MaLiaisonPage from './MaLiaisonPage'

afterEach(cleanup)
beforeAll(() => {
  URL.createObjectURL = vi.fn(() => 'blob:attestation')
  URL.revokeObjectURL = vi.fn()
  HTMLAnchorElement.prototype.click = vi.fn()
})

const ouvrir = (chemin: string, o: { langue?: Langue; services?: Services } = {}) =>
  rendrePage(<MaLiaisonPage />, { chemin, motif: chemin.startsWith('/portail/liaisons/') ? '/portail/liaisons/:id' : '/portail/liaisons', ...o })

describe('POR-03 Ma liaison', () => {
  it('POR-03 : chargement puis liste cloisonnée à l\'institution du jeton', async () => {
    const { container } = ouvrir('/portail/liaisons')
    expect(container.querySelector('.gm-skeleton')).not.toBeNull()
    expect(await screen.findByText('LIA-2026-017')).toBeTruthy()
    expect(screen.queryByText('LIA-2025-102')).toBeNull()
  })

  it('POR-03 : vide quand l\'institution n\'a aucune liaison', async () => {
    const services = { ...servicesSimules({ latenceMs: 0 }) }
    services.portail = { ...services.portail, mesLiaisons: () => Promise.resolve([]) }
    ouvrir('/portail/liaisons', { services })
    expect(await screen.findByText(/Aucun/)).toBeTruthy()
  })

  it('POR-03 : erreur avec identifiant de corrélation', async () => {
    const services = { ...servicesSimules({ latenceMs: 0 }) }
    services.portail = { ...services.portail, maLiaison: () => Promise.reject(new ApiError(404, 'HOM_LIAISON_NOT_FOUND', 'Introuvable.', 'cid-404')) }
    ouvrir('/portail/liaisons/LIA-2026-099', { services })
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('cid-404')
    expect(alerte.textContent).toContain('Ce parcours est introuvable.')
  })

  it('POR-03 : ce qui incombe à l\'institution, sans aucun champ interne', async () => {
    ouvrir('/portail/liaisons/LIA-2026-017')
    expect(await screen.findByText('Recette du service paiement')).toBeTruthy()
    const texte = document.body.textContent ?? ''
    expect(texte).not.toMatch(/Chef de projet|Ingénieur d'homologation|chefDeProjet|motifEtat/)
    expect(texte).not.toContain('Rapprochement à blanc')
  })

  it('POR-03 : l\'attestation téléchargée est vérifiée par son empreinte', async () => {
    ouvrir('/portail/liaisons/LIA-2026-017?onglet=certification')
    fireEvent.click(await screen.findByRole('button', { name: 'Télécharger l\'attestation' }))
    expect(await screen.findByText(/Empreinte vérifiée/)).toBeTruthy()
    expect(URL.createObjectURL).toHaveBeenCalled()
  })

  it('POR-03 : résultats du simulateur repris, jamais recalculés', async () => {
    ouvrir('/portail/liaisons/LIA-2026-017?onglet=resultats')
    expect(await screen.findByText('Résultats repris du simulateur, jamais recalculés.')).toBeTruthy()
    expect(within(screen.getByRole('table')).getByText('SC-LIT-01')).toBeTruthy()
  })

  it.each(['en', 'es'] as const)('POR-03 : rendu en %s sans clé brute', async (langue) => {
    ouvrir('/portail/liaisons/LIA-2026-017', { langue })
    await screen.findByText('Recette du service paiement')
    expect(document.body.textContent).not.toMatch(/\b(portail|campagne|attestation|partie)\.[a-zA-Z]/)
    expect(document.body.textContent).toContain(langue === 'en' ? 'What my institution must do' : 'Lo que corresponde a mi institución')
  })
})
