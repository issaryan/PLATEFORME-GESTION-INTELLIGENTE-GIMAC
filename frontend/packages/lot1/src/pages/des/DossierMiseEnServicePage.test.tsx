import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import DossierMiseEnServicePage from './DossierMiseEnServicePage'

afterEach(cleanup)

const MOTIF = '/des/liaisons/:id/mise-en-service'
const ouvrir = (liaison: string, o: { onglet?: string; langue?: Langue; services?: Services } = {}) =>
  rendrePage(<DossierMiseEnServicePage />, { chemin: `/des/liaisons/${liaison}/mise-en-service${o.onglet ? `?onglet=${o.onglet}` : ''}`, motif: MOTIF, langue: o.langue, services: o.services })

const avec = (surcharge: (s: Services) => Services) => surcharge(servicesSimules({ latenceMs: 0 }))

describe('DES-11 Dossier de mise en service', () => {
  it('DES-11 : chargement, puis nominal avec l\'institution en titre', async () => {
    const { container } = ouvrir('LIA-2025-102')
    expect(container.querySelector('[aria-busy="true"]')).not.toBeNull()
    expect(await screen.findByRole('heading', { level: 1, name: 'Banque B' })).toBeTruthy()
  })

  it('DES-11 : vide quand la liaison n\'a pas encore de dossier', async () => {
    ouvrir('LIA-2026-017')
    expect(await screen.findByText(/Aucun dossier de mise en service pour la liaison LIA-2026-017/)).toBeTruthy()
  })

  it('DES-11 : erreur avec identifiant de corrélation et réessai', async () => {
    const services = avec((s) => ({ ...s, certification: { ...s.certification, dossierParLiaison: () => Promise.reject(new ApiError(503, 'CER_INDISPONIBLE', 'Service indisponible.', 'cid-503')) } }))
    ouvrir('LIA-2025-102', { services })
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('cid-503')
    expect(within(alerte).getByRole('button', { name: 'Réessayer' })).toBeTruthy()
  })

  it('DES-11 : quatre quitus, sécurité et conformité au même rang, signature du seul quitus permis', async () => {
    const { container } = ouvrir('LIA-2025-102', { onglet: 'circuit' })
    await screen.findByText(/Socle d'évaluation signé/)
    const etapes = container.querySelectorAll('.gm-quitus > li')
    expect(etapes).toHaveLength(4)
    expect([...etapes].filter((li) => li.textContent?.includes('en parallèle'))).toHaveLength(2)
    const boutons = within(screen.getByTestId('quitus-signables')).getAllByRole('button')
    expect(boutons.map((b) => b.textContent)).toEqual(['Signer le quitus de sécurité'])
  })

  it('DES-11 : un socle périmé (CER_SOCLE_PERIME) est expliqué avec son identifiant de corrélation', async () => {
    const services = avec((s) => ({
      ...s,
      reauth: { assertion: () => Promise.resolve('YXNzZXJ0') },
      certification: { ...s.certification, signerQuitus: () => Promise.reject(new ApiError(409, 'CER_SOCLE_PERIME', 'Socle périmé.', 'cid-409')) },
    }))
    ouvrir('LIA-2025-102', { onglet: 'circuit', services })
    fireEvent.click(await screen.findByRole('button', { name: 'Signer le quitus de sécurité' }))
    const dialogue = await screen.findByRole('dialog')
    fireEvent.change(within(dialogue).getByRole('textbox'), { target: { value: 'Contrôles de sécurité conformes au référentiel.' } })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(() => expect(dialogue.textContent).toContain('Le socle d\'évaluation a changé'))
    expect(dialogue.textContent).toContain('cid-409')
  })

  it('DES-11 : les six conditions de complétude', async () => {
    ouvrir('LIA-2025-102', { onglet: 'completude' })
    expect(await screen.findByText('Quatre quitus favorables')).toBeTruthy()
    for (const c of ['Couverture de certification complète', 'Couverture de mise en service complète', 'Fiche de changement approuvée', 'Procès-verbal de bascule déposé', 'Empreintes des pièces vérifiées']) {
      expect(screen.getByText(c)).toBeTruthy()
    }
  })

  it.each(['en', 'es'] as const)('DES-11 : rendu en %s sans clé brute', async (langue) => {
    ouvrir('LIA-2025-102', { onglet: 'circuit', langue })
    await screen.findByRole('heading', { level: 1, name: 'Banque B' })
    await screen.findByTestId('quitus-signables')
    expect(document.body.textContent).not.toMatch(/\b(dossier|quitus|condition|fiche|pv|attestation|registre)\.[a-zA-Z]/)
    expect(document.body.textContent).toContain(langue === 'en' ? 'Quitus circuit' : 'Circuito de los quitus')
  })
})
