import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { Route, Routes, useLocation } from 'react-router'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import DossierMiseEnServicePage from './DossierMiseEnServicePage'

afterEach(cleanup)

const MOTIF = '/des/liaisons/:id/mise-en-service'
function Lieu() {
  const l = useLocation()
  return <output data-testid="lieu">{l.pathname + l.search}</output>
}
const ouvrir = (liaison: string, o: { onglet?: string; langue?: Langue; services?: Services } = {}) =>
  rendrePage(
    <Routes>
      <Route path={MOTIF} element={<DossierMiseEnServicePage />} />
      <Route path="*" element={<Lieu />} />
    </Routes>,
    { chemin: `/des/liaisons/${liaison}/mise-en-service${o.onglet ? `?onglet=${o.onglet}` : ''}`, motif: '*', langue: o.langue, services: o.services },
  )

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

  it('DES-11 : modèle hybride (D-56), plus de circuit des quatre quitus ; l\'onglet Décision conduit au dossier de la liaison', async () => {
    const { container } = ouvrir('LIA-2025-102', { onglet: 'decision' })
    expect(await screen.findByText('Décision de mise en production : modèle hybride')).toBeTruthy()
    expect(screen.queryByRole('tab', { name: 'Circuit des quitus' })).toBeNull()
    expect(screen.getByRole('tab', { name: 'Décision' })).toBeTruthy()
    expect(container.querySelector('.gm-quitus')).toBeNull()
    expect(await screen.findByText('L\'état de la décision n\'est pas disponible pour cette liaison.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la décision de la liaison' }))
    expect((await screen.findByTestId('lieu')).textContent).toBe('/des/parcours/LIA-2025-102?onglet=decision')
  })

  it('DES-11 : une ancienne adresse ?onglet=circuit ouvre l\'onglet Décision', async () => {
    ouvrir('LIA-2025-102', { onglet: 'circuit' })
    expect(await screen.findByText('Décision de mise en production : modèle hybride')).toBeTruthy()
    expect(screen.getByRole('tab', { name: 'Décision' }).getAttribute('aria-selected')).toBe('true')
  })

  it('DES-11 : résumé du procès-verbal, des visas et de l\'approbation de la liaison', async () => {
    const services = avec((s) => ({
      ...s,
      certification: { ...s.certification, dossierParLiaison: () => s.certification.dossierParLiaison('LIA-2025-102').then((d) => (d ? { ...d, liaisonId: 'LIA-2026-019' } : d)) },
    }))
    ouvrir('LIA-2025-102', { onglet: 'decision', services })
    const resume = await screen.findByTestId('resume-decision')
    await waitFor(() => expect(resume.textContent).toContain('Signé des deux parties'))
    expect(resume.textContent).toContain('Visa de la conformité')
    expect(resume.textContent).toContain('Dossier constitué')
  })

  it('DES-11 : l\'onglet suit l\'adresse (CER-08 à CER-15 accessibles)', async () => {
    ouvrir('LIA-2025-102', { onglet: 'decision' })
    fireEvent.click(await screen.findByRole('tab', { name: 'Procès-verbal' }))
    expect(await screen.findByRole('tab', { name: 'Procès-verbal', selected: true })).toBeTruthy()
    for (const o of ['Complétude', 'Fiche de changement', 'Horodatages', 'Attestation', 'Registre']) expect(screen.getByRole('tab', { name: o })).toBeTruthy()
  })

  it('DES-11 : les six conditions de complétude', async () => {
    ouvrir('LIA-2025-102', { onglet: 'completude' })
    expect(await screen.findByText('Couverture de certification complète')).toBeTruthy()
    for (const c of [ 'Couverture de mise en service complète', 'Fiche de changement approuvée', 'Procès-verbal de bascule déposé', 'Empreintes des pièces vérifiées']) {
      expect(screen.getByText(c)).toBeTruthy()
    }
  })

  it.each(['en', 'es'] as const)('DES-11 : rendu en %s sans clé brute', async (langue) => {
    ouvrir('LIA-2025-102', { onglet: 'decision', langue })
    await screen.findByRole('heading', { level: 1, name: 'Banque B' })
    await screen.findByTestId('resume-decision')
    expect(document.body.textContent).not.toMatch(/\b(dossier|quitus|condition|fiche|pv|attestation|registre|decision)\.[a-zA-Z]/)
    expect(screen.getByRole('tab', { name: langue === 'en' ? 'Decision' : 'Decisión' })).toBeTruthy()
  })
})
