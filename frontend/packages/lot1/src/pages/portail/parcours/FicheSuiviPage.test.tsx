import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import FicheSuiviPage, { manquantsFiche } from './FicheSuiviPage'
import { auditer, ouvrir, sansCleNiNomInterne, services } from './outilsTest'

afterEach(cleanup)

const MOTIF = '/portail/liaisons/:id/fiche-suivi'
const monter = (id: string, o: Parameters<typeof ouvrir>[3] = {}) => ouvrir(<FicheSuiviPage />, `/portail/liaisons/${id}/fiche-suivi`, MOTIF, o)

describe('Portail : fiche de suivi des services (étape 10, rang 32)', () => {
  it('tous les champs obligatoires : choix et commentaire par service', () => {
    const lignes = [{ serviceCode: 'A', libelle: 'A', ouvert: null, commentaire: null }, { serviceCode: 'B', libelle: 'B', ouvert: null, commentaire: null }]
    expect(manquantsFiche(lignes, { A: { ouvert: 'oui', commentaire: 'ok' }, B: { ouvert: 'non', commentaire: ' ' } }).map((l) => l.serviceCode)).toEqual(['B'])
    expect(manquantsFiche(lignes, { A: { ouvert: '', commentaire: 'ok' }, B: { ouvert: 'non', commentaire: 'reporté' } }).map((l) => l.serviceCode)).toEqual(['A'])
  })

  it('pré-remplie avec la valeur connue ; manquants signalés ; confirmation ; lecture de la fiche transmise', async () => {
    const { container } = monter('LIA-2025-088')
    expect(await screen.findByText('Service par service, indiquez ce qui est effectivement ouvert')).toBeTruthy()
    const oui = screen.getAllByRole('radio', { name: 'Oui, ouvert' }) as HTMLInputElement[]
    expect(oui.map((r) => r.checked)).toEqual([true, false, false])
    await auditer(container)
    fireEvent.click(screen.getByRole('button', { name: 'Transmettre la fiche' }))
    expect(await screen.findByText('3 services restent à renseigner')).toBeTruthy()
    expect(screen.getAllByText('Indiquez si le service est ouvert.')).toHaveLength(2)
    fireEvent.click(screen.getAllByRole('radio', { name: 'Non, pas encore' })[1])
    fireEvent.click(screen.getAllByRole('radio', { name: 'Oui, ouvert' })[2])
    screen.getAllByRole('textbox').forEach((z, i) => fireEvent.change(z, { target: { value: `Commentaire ${i + 1}` } }))
    fireEvent.click(screen.getByRole('button', { name: 'Transmettre la fiche' }))
    const dialogue = await screen.findByRole('alertdialog')
    expect(within(dialogue).getByText(/2 service\(s\) déclaré\(s\) ouvert\(s\) sur 3/)).toBeTruthy()
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Transmettre la fiche' }))
    expect(await screen.findByText('Fiche transmise')).toBeTruthy()
    expect(screen.getByText('Non ouvert')).toBeTruthy()
    expect(screen.getByText('Commentaire 2')).toBeTruthy()
    expect(screen.getByText('en attente')).toBeTruthy()
  })

  it('fiche transmise au service : lignes envoyées telles que saisies', async () => {
    const renseigner = vi.fn(() => Promise.resolve())
    monter('LIA-2025-088', { services: services(() => ({ renseignerFicheSuivi: renseigner })) })
    await screen.findByText('Service par service, indiquez ce qui est effectivement ouvert')
    screen.getAllByRole('radio', { name: 'Non, pas encore' }).slice(1).forEach((r) => fireEvent.click(r))
    screen.getAllByRole('textbox').forEach((z) => fireEvent.change(z, { target: { value: 'Constat au 29 septembre' } }))
    fireEvent.click(screen.getByRole('button', { name: 'Transmettre la fiche' }))
    fireEvent.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Transmettre la fiche' }))
    await vi.waitFor(() => expect(renseigner).toHaveBeenCalledWith('LIA-2025-088', [
      { serviceCode: 'PAIEMENT_MARCHAND', ouvert: true, commentaire: 'Constat au 29 septembre' },
      { serviceCode: 'REMITTANCE', ouvert: false, commentaire: 'Constat au 29 septembre' },
      { serviceCode: 'FACTURE', ouvert: false, commentaire: 'Constat au 29 septembre' },
    ]))
  })

  it('pas encore remise : état explicite', async () => {
    monter('LIA-2026-030')
    expect(await screen.findByText('La fiche ne vous a pas encore été remise')).toBeTruthy()
    sansCleNiNomInterne()
  })

  it('sans HOM_PORTAL_SUBMIT : pas de saisie', async () => {
    monter('LIA-2025-088', { sans: ['HOM_PORTAL_SUBMIT'] })
    expect(await screen.findByText(/pas de l'accomplir/)).toBeTruthy()
    expect(screen.queryByRole('radio')).toBeNull()
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    monter('LIA-2025-088', { langue })
    await screen.findByText(langue === 'en' ? 'Submit the sheet' : 'Transmitir la ficha')
    sansCleNiNomInterne()
  })
})
