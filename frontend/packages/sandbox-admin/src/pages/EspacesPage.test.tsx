import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import EspacesPage from './EspacesPage'
import EspacePage from './EspacePage'
import { LECTEUR, rendre, simules } from '../test/rendu'
import { CORRESPONDANT_99004, ESPACES_DEMO } from '../services/simule'
import type { Espace } from '../modele'

// « Espaces des institutions » (D-118 point 3) sur SBX-ESPACE simulé au contrat exact.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-10-22T09:00:00Z'))
})
afterEach(() => vi.useRealTimers())

const beaucoup = (): Espace[] => Array.from({ length: 23 }, (_, i) => ({ ...ESPACES_DEMO[0], codeParticipant: String(98000 + i), codeEspace: `ESP-${98000 + i}` }))

describe('EspacesPage', () => {
  it('liste des espaces : codes, états traduits, statut au registre, administrateurs', async () => {
    rendre(<EspacesPage />)
    expect(await screen.findByText('ESP-99001')).toBeTruthy()
    expect(screen.getByRole('heading', { level: 1, name: 'Espaces des institutions' })).toBeTruthy()
    expect(screen.getByText('Ouvert')).toBeTruthy()
    expect(screen.getAllByText('Suspendu').length).toBeGreaterThan(0)
    expect(screen.getByText('En homologation')).toBeTruthy()
    expect(screen.getByText('Retiré')).toBeTruthy()
    expect(screen.getByText('Page 1, 20 espaces au plus par page')).toBeTruthy()
  })

  it('pagination par curseur : page suivante puis précédente', async () => {
    rendre(<EspacesPage />, { services: simules({ espaces: beaucoup() }) })
    expect(await screen.findByText('ESP-98000')).toBeTruthy()
    const precedente = screen.getByRole('button', { name: 'Page précédente' }) as HTMLButtonElement
    expect(precedente.disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Page suivante' }))
    expect(await screen.findByText('ESP-98020')).toBeTruthy()
    expect(screen.queryByText('ESP-98000')).toBeNull()
    expect((screen.getByRole('button', { name: 'Page suivante' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Page précédente' }))
    expect(await screen.findByText('ESP-98000')).toBeTruthy()
  })

  it('ouverture d’un espace par l’administrateur : saisie contrôlée, espace listé, résultat annoncé', async () => {
    rendre(<EspacesPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir un espace' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/^Code participant/), { target: { value: '9900' } })
    expect(within(d).getByText('Le code participant compte cinq chiffres.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Code participant/), { target: { value: '99004' } })
    fireEvent.change(within(d).getByLabelText(/^Correspondant/), { target: { value: 'pas-un-uuid' } })
    expect(within(d).getByText('L’identifiant d’un compte est un UUID.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Correspondant/), { target: { value: CORRESPONDANT_99004 } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(await screen.findByText('L’espace de l’institution 99004 est ouvert.')).toBeTruthy()
    expect(await screen.findByText('ESP-99004')).toBeTruthy()
  })

  it('ouverture refusée : problème traduit dans le dialogue, qui reste ouvert', async () => {
    rendre(<EspacesPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir un espace' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/^Code participant/), { target: { value: '99001' } })
    fireEvent.change(within(d).getByLabelText(/^Correspondant/), { target: { value: CORRESPONDANT_99004 } })
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir l’espace' }))
    expect(await within(d).findByText('L’institution possède déjà un espace')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
  })

  it('lecteur sans droit d’administration : liste seule, aucune ouverture proposée ; une ligne ouvre la fiche', async () => {
    rendre(<EspacesPage />, { permissions: LECTEUR })
    fireEvent.click(await screen.findByText('ESP-99002'))
    expect(await screen.findByText('fiche ouverte')).toBeTruthy()
  })

  it('lecteur : aucun bouton d’ouverture', async () => {
    rendre(<EspacesPage />, { permissions: LECTEUR })
    expect(await screen.findByText('ESP-99001')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ouvrir un espace' })).toBeNull()
  })

  it('aucun espace ; espace en ouverture sans statut ni date ; service en échec puis nouvelle tentative', async () => {
    const r = rendre(<EspacesPage />, { services: simules({ espaces: [] }) })
    expect(await screen.findByText('Aucun espace')).toBeTruthy()
    r.unmount()
    const r2 = rendre(<EspacesPage />, { services: simules({ espaces: [{ ...ESPACES_DEMO[0], etat: 'EN_OUVERTURE', statutRegistre: null, dateOuverture: null }] }) })
    expect(await screen.findByText('En ouverture')).toBeTruthy()
    expect(screen.getByText('Inconnu')).toBeTruthy()
    expect(screen.getByText('Non ouvert')).toBeTruthy()
    r2.unmount()
    const s = simules()
    let essais = 0
    const services = { ...s, espaces: { ...s.espaces, lister: async (q: { apres?: string; taille?: number }) => { essais++; if (essais === 1) throw new Error('panne'); return s.espaces.lister(q) } } }
    rendre(<EspacesPage />, { services })
    fireEvent.click(await screen.findByRole('button', { name: 'Réessayer' }))
    expect(await screen.findByText('ESP-99001')).toBeTruthy()
  })

  it('ouverture abandonnée : le dialogue se ferme sans rien ouvrir', async () => {
    rendre(<EspacesPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir un espace' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Annuler' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('anglais et espagnol', async () => {
    const r = rendre(<EspacesPage />, { langue: 'en' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Institution spaces' })).toBeTruthy()
    r.unmount()
    rendre(<EspacesPage />, { langue: 'es' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Espacios de las instituciones' })).toBeTruthy()
  })

  it('fiche d’un espace (/des/sandbox/espaces/:code) : retour vers la liste', async () => {
    rendre(<EspacePage />, { chemin: '/des/sandbox/fiche/99001', route: '/des/sandbox/fiche/:code' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Espace de l’institution 99001' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '← Espaces des institutions' }).getAttribute('href')).toBe('/des/sandbox/espaces')
    expect(await screen.findByText('ESP-99001')).toBeTruthy()
  })
})
