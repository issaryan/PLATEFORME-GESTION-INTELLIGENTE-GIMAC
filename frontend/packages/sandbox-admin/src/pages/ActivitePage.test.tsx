import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import ActivitePage from './ActivitePage'
import { ADMIN, rendre, simules } from '../test/rendu'
import { PERMISSION_LIAISON } from '../activite/modele'
import { fr, en, es } from '../activite/i18n'

// Date figée : le jeu de démonstration s'arrête au 04/11/2026 ; la période par défaut couvre les trente jours précédents.
const MAINTENANT = new Date('2026-11-04T10:00:00Z')
const AVEC_LIAISONS = [...ADMIN, PERMISSION_LIAISON]
const route = { route: '/des/sandbox/activite', chemin: '/des/sandbox/activite' }

describe('ActivitePage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true, toFake: ['Date'] })
    vi.setSystemTime(MAINTENANT)
  })
  afterEach(() => vi.useRealTimers())

  it('montre les indicateurs, les graphes par interface, les verdicts, les taux et les délais', async () => {
    rendre(<ActivitePage />, { ...route, permissions: AVEC_LIAISONS })
    expect(await screen.findByRole('heading', { level: 1, name: 'Activité des tests' })).toBeTruthy()
    const indicateurs = await screen.findByRole('group', { name: 'Indicateurs de la période' })
    expect(within(indicateurs).getByText('Exécutions')).toBeTruthy()
    expect(await screen.findByText(/Calculé le/)).toBeTruthy()
    for (const titre of ['Mobile', 'ISO 20022', 'Carte (ISO 8583)', 'Verdicts par statut', 'Taux de réussite des campagnes', 'Cas les plus défavorables']) {
      expect((await screen.findAllByText(titre)).length).toBeGreaterThan(0)
    }
    expect(await screen.findByText('Favorables')).toBeTruthy()
    expect(await screen.findByText('RC09 · ISO 20022')).toBeTruthy()
    expect(screen.queryByText(/Source indisponible/)).toBeNull()
    expect(await screen.findAllByRole('link', { name: 'Ouvrir le dossier' })).not.toHaveLength(0)
    expect((await screen.findAllByText('CMP-99001-CARTE')).length).toBeGreaterThan(0)
    expect(await screen.findAllByText(/Ouverte depuis/)).not.toHaveLength(0)
    await waitFor(() => expect(document.querySelectorAll('.gm-state-done').length).toBeGreaterThan(0))
    expect(screen.queryByText(/Chaîne réduite à la sandbox/)).toBeNull()
  })

  it('ouvre l’onglet sandbox du dossier depuis la barre d’une campagne', async () => {
    rendre(<ActivitePage />, { ...route })
    const barre = await screen.findByRole('button', { name: /CMP-99001-CARTE · 99001/ })
    fireEvent.click(barre)
    expect(await screen.findByText('dossier ouvert')).toBeTruthy()
  })

  it('filtre par période, participant et interface depuis l’adresse et la barre de filtres', async () => {
    rendre(<ActivitePage />, { route: '/des/sandbox/activite', chemin: '/des/sandbox/activite?periode=365&participant=99004&interface=CARTE' })
    expect(await screen.findByText('Participant : 99004')).toBeTruthy()
    expect(screen.getByText('Interface : Carte (ISO 8583)')).toBeTruthy()
    expect(screen.getAllByText('Mobile')).toHaveLength(1)
    expect(await screen.findAllByText('CMP-99004-CARTE')).not.toHaveLength(0)
    expect(await screen.findAllByText('50 jours')).not.toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Interface'), { target: { value: '' } })
    await waitFor(() => expect(screen.getAllByText('Mobile').length).toBeGreaterThan(1))
    fireEvent.click(screen.getAllByRole('button', { name: /Retirer|Supprimer|×/ })[0])
    await waitFor(() => expect(screen.queryByText('Participant : 99004')).toBeNull())
    fireEvent.change(screen.getByLabelText('Période'), { target: { value: '7' } })
    // La période est toujours choisie : pas d'option « Tous » ; aucune recherche libre ni vue enregistrée inerte.
    expect(Array.from((screen.getByLabelText('Période') as HTMLSelectElement).options).map((o) => o.value)).toEqual(['7', '30', '90', '365'])
    expect(screen.queryByRole('searchbox')).toBeNull()
    expect(screen.queryByRole('button', { name: /Enregistrer/ })).toBeNull()
    await waitFor(() => expect(Array.from((screen.getByLabelText('Participant') as HTMLSelectElement).options).some((o) => o.value === '99001')).toBe(true))
    fireEvent.change(screen.getByLabelText('Participant'), { target: { value: '99001' } })
    expect(await screen.findByText('Participant : 99001')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /Réinitialiser/ }))
    await waitFor(() => expect(screen.queryByText('Participant : 99001')).toBeNull())
  })

  it('passe en mode dégradé sans la lecture des liaisons et signale une liaison non lue', async () => {
    rendre(<ActivitePage />, { ...route })
    expect(await screen.findByText(/Chaîne réduite à la sandbox/)).toBeTruthy()
    const services = simules({ permissions: AVEC_LIAISONS, liaisons: [] })
    rendre(<ActivitePage />, { ...route, permissions: AVEC_LIAISONS, services })
    expect((await screen.findAllByText('Liaison non lue')).length).toBeGreaterThan(0)
  })

  it('montre un état hors chaîne (liaison gelée) et les codes de rejet quand la source les porte', async () => {
    const services = simules({
      permissions: AVEC_LIAISONS,
      codesRetour: [{ interface: 'ISO20022', code: 'RC09', occurrences: 12, participants: 2, dernierLe: '2026-11-03T10:00:00Z' }],
    })
    rendre(<ActivitePage />, { route: '/des/sandbox/activite', chemin: '/des/sandbox/activite?periode=90', permissions: AVEC_LIAISONS, services })
    expect(await screen.findByText('RC09 · ISO 20022')).toBeTruthy()
    expect((await screen.findAllByText('Gelée')).length).toBeGreaterThan(0)
    cleanup()
    rendre(<ActivitePage />, { ...route, services: simules({ codesRetour: null }) })
    expect(await screen.findByText(/Source indisponible/)).toBeTruthy()
  })

  it('affiche les listes vides et propose une nouvelle tentative après une panne', async () => {
    const vide = rendre(<ActivitePage />, { ...route, services: simules({ executions: [], codesRetour: [] }) })
    expect(await screen.findByText('Aucun participant actif')).toBeTruthy()
    expect(screen.getAllByText('Aucune campagne sur la période').length).toBeGreaterThan(0)
    expect(await screen.findByText('Aucun code de rejet sur la période')).toBeTruthy()
    vide.unmount()
    const s = simules()
    const panne = () => Promise.reject(new Error('panne'))
    rendre(<ActivitePage />, { ...route, services: { ...s, activite: { ...s.activite, synthese: panne, activite: panne, campagnes: panne, cas: panne, codesRetour: panne } } })
    await waitFor(() => expect(screen.getAllByRole('button', { name: 'Réessayer' }).length).toBeGreaterThanOrEqual(6))
    fireEvent.click(screen.getAllByRole('button', { name: 'Réessayer' })[0])
  })

  it('refuse sans la permission de supervision', async () => {
    rendre(<ActivitePage />, { ...route, permissions: ['PIL_ACTIVITY_OWN'] })
    expect((await screen.findAllByRole('button', { name: 'Réessayer' })).length).toBeGreaterThan(0)
  })

  it('traductions : mêmes clés en français, anglais et espagnol, aucune vide', () => {
    const cles = Object.keys(fr).sort()
    expect(Object.keys(en).sort()).toEqual(cles)
    expect(Object.keys(es).sort()).toEqual(cles)
    for (const l of [fr, en, es]) expect(Object.values(l).every((v) => v.trim().length > 0)).toBe(true)
  })
})
