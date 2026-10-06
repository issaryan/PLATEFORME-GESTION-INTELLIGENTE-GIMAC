import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import SupervisionPage from './SupervisionPage'
import { SUPER_ADMIN, avec, rendrePage } from '../test/rendu'
import { versCsv } from '../composants/graphiquesSupervision'
import { series } from '../services/simule/donnees'
import type { SeriesMetriques } from '../modele/plateforme'

afterEach(cleanup)
beforeEach(() => window.localStorage.clear())

const page = (o: Partial<Parameters<typeof rendrePage>[1]> = {}) =>
  rendrePage(<SupervisionPage />, { chemin: '/des/plateforme/supervision', motif: '/des/plateforme/supervision', permissions: SUPER_ADMIN, ...o })

// Centre de contrôle : graphiques des métriques historisées en plus des tableaux de l'état instantané.
describe('Centre de contrôle de la supervision', () => {
  it('anneau de disponibilité, tendances par service, courbes de la plateforme, des services et des serveurs', async () => {
    page()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Centre de contrôle')
    expect(await screen.findByRole('img', { name: 'Disponibilité des services : 88 %' })).toBeTruthy()
    expect(screen.getByText('7 sur 8 démarrés')).toBeTruthy()
    expect(await screen.findByRole('columnheader', { name: 'Tendance de latence' })).toBeTruthy()
    expect((await screen.findAllByRole('img', { name: /^Latence p95 par service · .+ : de .+ ms à .+ ms$/ })).length).toBeGreaterThan(0)
    for (const titre of ['Charge processeur de la plateforme', 'Latence p95 par service', 'Débit par service', 'Erreurs serveur par service', 'Mémoire Java par service', 'Processeur des serveurs', 'Mémoire des serveurs', 'Disque des serveurs', 'Mémoire par conteneur']) {
      expect(await screen.findByRole('region', { name: titre })).toBeTruthy()
    }
    expect(screen.getByRole('region', { name: 'Courbes des services' })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Courbes des serveurs et conteneurs' })).toBeTruthy()
  })

  it('période choisie en tête : s’applique aux graphiques et est mémorisée', async () => {
    page()
    const sixHeures = await screen.findByRole('radio', { name: '6 heures' })
    fireEvent.click(sixHeures)
    expect((await screen.findAllByText(/· 6 heures$/)).length).toBeGreaterThan(5)
    expect(window.localStorage.getItem('gimac.supervision.periode')).toBe('SIX_HEURES')
    cleanup()
    page()
    expect(((await screen.findByRole('radio', { name: '6 heures' })) as HTMLInputElement).checked).toBe(true)
  })

  it('fiche d’un service au clic sur sa ligne : ses quatre courbes', async () => {
    page()
    const entete = await screen.findByRole('columnheader', { name: 'Tendance de latence' })
    const tableau = entete.closest('table') as HTMLTableElement
    const ligne = within(tableau).getAllByRole('row')[1]
    const nom = ligne.querySelector('.gm-mono')?.textContent ?? ''
    fireEvent.click(ligne)
    const fiche = await screen.findByRole('dialog')
    expect(within(fiche).getByText(`Courbes du service ${nom}`)).toBeTruthy()
    expect(await within(fiche).findByRole('region', { name: 'Débit' })).toBeTruthy()
    expect(within(fiche).getByRole('tab', { name: 'Courbes' })).toBeTruthy()
    expect(within(fiche).getAllByRole('region').length).toBe(4)
    fireEvent.click(within(fiche).getByRole('button', { name: 'Fermer' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('Prometheus injoignable : message explicite, le reste de la supervision reste affiché', async () => {
    const indisponible = (m: SeriesMetriques['metrique'], p: SeriesMetriques['periode']): Promise<SeriesMetriques> =>
      Promise.resolve({ metrique: m, unite: '%', periode: p, debut: '', fin: '', pasSecondes: 15, disponible: false, series: [] })
    page({ services: avec('plateforme', { series: indisponible }) })
    await waitFor(() => expect(screen.getAllByText('Mesures historiques indisponibles : Prometheus ne répond pas.').length).toBe(9))
    expect(await screen.findByRole('img', { name: 'Disponibilité des services : 88 %' })).toBeTruthy()
  })

  it('aucune mesure de conteneur : la collecte cAdvisor est signalée ; export CSV des séries', async () => {
    const vides = (m: SeriesMetriques['metrique'], p: SeriesMetriques['periode']): Promise<SeriesMetriques> =>
      Promise.resolve({ ...series(m, p), series: m.startsWith('CONTENEUR_') || m === 'PLATEFORME_CPU' ? [] : series(m, p).series })
    page({ services: avec('plateforme', { series: vides }) })
    await waitFor(() => expect(screen.getAllByText(/la collecte cAdvisor n'est pas active/).length).toBe(2))
    const creer = vi.fn(() => 'blob:x')
    const revoquer = vi.fn()
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: creer, revokeObjectURL: revoquer }))
    const debit = await screen.findByRole('region', { name: 'Débit par service' })
    fireEvent.click(within(debit).getByRole('button', { name: /Exporter/ }))
    expect(creer).toHaveBeenCalled()
    expect(revoquer).toHaveBeenCalledWith('blob:x')
    vi.unstubAllGlobals()
  })

  it('CSV : une ligne par instant, une colonne par série, mesure absente vide, séparateur échappé', () => {
    const csv = versCsv(
      { metrique: 'SERVICE_DEBIT', unite: 'req/s', periode: 'UNE_HEURE', debut: '', fin: '', pasSecondes: 15, disponible: true,
        series: [{ nom: 'iam;1', points: [{ t: Date.UTC(2026, 9, 6, 8), v: 1.5 }, { t: Date.UTC(2026, 9, 6, 8, 0, 15), v: null }] }] },
      'fr',
    )
    const lignes = csv.split('\n')
    expect(lignes[0]).toBe('instant;"iam;1"')
    expect(lignes).toHaveLength(3)
    expect(lignes[1].endsWith(';1.5')).toBe(true)
    expect(lignes[2].endsWith(';')).toBe(true)
  })
})
