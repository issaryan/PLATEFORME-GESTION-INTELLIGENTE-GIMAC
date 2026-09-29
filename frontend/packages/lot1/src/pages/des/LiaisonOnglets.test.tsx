import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import LiaisonPage from './LiaisonPage'
import { rendrePage } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

// DES-09 : onglets de la liaison (opérations et désignation de leur responsable, réouverture motivée,
// qualification des scénarios inconnus, preuves exportées, périmètre non calculable) et actions de la fiche.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const page = (id: string, onglet: string, services?: Services) =>
  rendrePage(<LiaisonPage />, { chemin: `/des/liaisons/${id}?onglet=${onglet}`, motif: '/des/liaisons/:id', services: services ?? servicesSimules({ latenceMs: 0 }) })
const avec = (surcharge: Partial<Services['homologation']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, homologation: { ...s.homologation, ...surcharge } }
}

describe('DES-09 : onglets et actions de la liaison', () => {
  it('opérations : désignation du responsable avec motif', async () => {
    const attribuer = vi.fn(async () => undefined)
    const { container } = page('LIA-2026-017', 'operations', avec({ attribuerOperation: attribuer }))
    fireEvent.click(await screen.findByRole('button', { name: "Attribuer l'opération" }))
    expect(await screen.findByText(/de l'opération OP-E03/)).toBeTruthy()
    await waitFor(() => expect(container.querySelector('select[name="responsableId"] option[value="U-102"]')).toBeTruthy())
    fireEvent.change(container.querySelector('select[name="responsableId"]')!, { target: { value: 'U-102' } })
    fireEvent.change(container.querySelector('textarea[name="motif"]')!, { target: { value: 'Charge rééquilibrée' } })
    fireEvent.submit(container.querySelector('form')!)
    await waitFor(() => expect(attribuer).toHaveBeenCalledWith('OPI-017-5', 'U-102', 'Charge rééquilibrée'))
    await waitFor(() => expect(container.querySelector('form')).toBeNull())
  })
  it('opérations : réouverture motivée d’une opération close', async () => {
    const rouvrir = vi.fn(async () => undefined)
    page('LIA-2026-017', 'operations', avec({ rouvrirOperation: rouvrir }))
    fireEvent.click((await screen.findAllByRole('button', { name: 'Rouvrir l\'opération' }))[0])
    const d = await screen.findByRole('alertdialog')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Preuve contestée par la production' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Rouvrir l\'opération' }))
    await waitFor(() => expect(rouvrir).toHaveBeenCalledWith('OPI-017-1', 'Preuve contestée par la production'))
    const toast = await screen.findByRole('status')
    fireEvent.click(within(toast.parentElement!).getByRole('button', { name: 'Fermer' }))
    await waitFor(() => expect(screen.queryByText(/effectué/)).toBeNull())
  })
  it('résultats : scénario inconnu déclaré au référentiel, ou qualifié hors parcours depuis son tiroir', async () => {
    const qualifier = vi.fn(async () => undefined)
    page('LIA-2026-017', 'resultats', avec({ qualifierScenario: qualifier }))
    fireEvent.click(await screen.findByRole('button', { name: 'Déclarer au référentiel' }))
    let d = await screen.findByRole('alertdialog')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Scénario à ajouter' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Déclarer au référentiel' }))
    await waitFor(() => expect(qualifier).toHaveBeenCalledWith('SCI-0091', 'A_DECLARER', 'Scénario à ajouter'))
    const lignes = screen.getAllByRole('button', { name: "Plus d'actions" })
    fireEvent.click(lignes[lignes.length - 1])
    fireEvent.click(await screen.findByRole('button', { name: 'Qualifier hors parcours' }))
    d = await screen.findByRole('alertdialog')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Scénario de test interne' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Qualifier hors parcours' }))
    await waitFor(() => expect(qualifier).toHaveBeenCalledWith('SCI-0091', 'HORS_PARCOURS', 'Scénario de test interne'))
  })
  it('preuves : dernier export scellé et opérations avec preuve', async () => {
    page('LIA-2025-102', 'preuves')
    expect(await screen.findByText('EXP-2026-0019')).toBeTruthy()
    expect(screen.getByText('scellé')).toBeTruthy()
    expect(screen.getByText('Opérations et leurs preuves')).toBeTruthy()
  })
  it('preuves : aucun export ; export du dossier depuis la fiche', async () => {
    const exporter = vi.fn(async () => undefined)
    page('LIA-2026-017', 'preuves', avec({ exporterDossierDePreuves: exporter }))
    expect(await screen.findByText('Aucun dossier de preuves exporté')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Exporter le dossier de preuves' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Exporter le dossier de preuves' }))
    await waitFor(() => expect(exporter).toHaveBeenCalledWith('LIA-2026-017'))
  })
  it('périmètre : couverture non calculable signalée', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const l = await s.homologation.lireLiaison('LIA-2026-030')
    page('LIA-2026-030', 'perimetre', avec({ lireLiaison: async () => ({ ...l, couvertureCertification: { taux: null, numerateur: 0, denominateur: 0, calculable: false, motifNonCalculable: 'aucun service souscrit' } }) }))
    expect(await screen.findByText(/aucun service souscrit/)).toBeTruthy()
  })
  it('dégel motivé d’une liaison gelée, puis chronologie', async () => {
    const degeler = vi.fn(async () => undefined)
    page('LIA-2026-021', 'detail', avec({ degelerLiaison: degeler }))
    fireEvent.click(await screen.findByRole('button', { name: 'Dégeler la liaison' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Suspension levée par la production monétique' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Dégeler la liaison' }))
    await waitFor(() => expect(degeler).toHaveBeenCalledWith('LIA-2026-021', 'Suspension levée par la production monétique'))
    fireEvent.click(screen.getByRole('tab', { name: 'Chronologie' }))
    expect(await screen.findByText('Liaison gelée : institution suspendue')).toBeTruthy()
  })
  it('bandeau de reprise suspendue quand le disjoncteur du simulateur est ouvert', async () => {
    page('LIA-2026-017', 'detail', avec({ etatReprise: async () => ({ etatDisjoncteur: 'OUVERT', depuis: '29/09/2026 10:00' }) }))
    expect(await screen.findByText(/29\/09\/2026 10:00/)).toBeTruthy()
  })
})
