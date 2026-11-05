import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import ActivitePage from '../pages/ActivitePage'
import { rendre, simules } from '../test/rendu'
import type { LigneCampagne, LigneCas, PageStatistiques } from './modele'

// Lignes sans code, sans liaison, sans interface ni jalon (projection reprise avant les jalons du 04/11/2026), et
// listes en panne : l'écran reste lisible et propose une nouvelle tentative.
const page = <T,>(elements: T[]): PageStatistiques<T> => ({ elements, total: elements.length, page: 0, taille: 10, suivant: null })
const taux = { numerateur: 1, denominateur: 2, valeur: '0.5000' }
const verdicts = { favorables: 1, defavorables: 1, nonExecutes: 0, enAttente: 0 }
const campagne: LigneCampagne = {
  identifiantCampagne: 'c-reprise', code: null, identifiantLiaison: null, codeParticipant: '99009', interfaceTest: null, etat: 'INCONNU', executions: 2,
  tauxReussite: taux, tauxAvancement: null, premierEssai: '2026-10-10T09:00:00Z', dernierEssai: '2026-10-11T09:00:00Z', ouverteLe: null, closeLe: null, versionBilan: null,
}
const cas: LigneCas = { codeCas: 'CAS-REPRIS', codeService: null, interfaceTest: null, executions: 2, verdicts, tauxReussite: taux }

describe('formes rares de l’écran Activité des tests', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true, toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-04T10:00:00Z'))
  })
  afterEach(() => vi.useRealTimers())

  it('campagne et cas repris sans code, liaison, interface ni jalon', async () => {
    const s = simules()
    const participant = (await s.activite.participants({ du: '2026-10-06T00:00:00Z', au: '2026-11-05T00:00:00Z' }, { page: 0, taille: 1 })).elements[0]
    const services = { ...s, activite: {
      ...s.activite,
      campagnes: async () => page([campagne]),
      cas: async () => page([cas]),
      participants: async () => page([{ ...participant, identifiantLiaison: null }]),
    } }
    rendre(<ActivitePage />, { route: '/des/sandbox/activite', chemin: '/des/sandbox/activite', services })
    expect(await screen.findByText('c-reprise · 99009')).toBeTruthy()
    expect(screen.getAllByText('c-reprise').length).toBeGreaterThan(0)
    expect(await screen.findByText('CAS-REPRIS')).toBeTruthy()
    expect(screen.getByText('Non datée')).toBeTruthy()
    expect(screen.getByText('INCONNU')).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Ouvrir le dossier' })).toBeNull()
  })

  it('listes paginées en panne : nouvelle tentative', async () => {
    const s = simules()
    const panne = () => Promise.reject(new Error('panne'))
    let appels = 0
    const services = { ...s, activite: { ...s.activite, participants: (...a: Parameters<typeof s.activite.participants>) => (a[1].taille === 100 ? s.activite.participants(...a) : (appels++, panne())) } }
    rendre(<ActivitePage />, { route: '/des/sandbox/activite', chemin: '/des/sandbox/activite', services: { ...services, activite: { ...services.activite, campagnes: (f, t, p) => (p.taille === 10 ? panne() : s.activite.campagnes(f, t, p)) } } })
    const boutons = await screen.findAllByRole('button', { name: /Réessayer/ })
    boutons.forEach((b) => fireEvent.click(b))
    expect(appels).toBeGreaterThan(0)
  })
})
