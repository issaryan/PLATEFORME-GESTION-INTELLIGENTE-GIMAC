import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import DossierLiaisonPage from '../DossierLiaisonPage'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { Services } from '../../../../services/ports'
import type { ResultatsSandbox } from '../../../../modele/sandbox'
import { PROFILS } from '../../../../permissions'
import { axeSansViolation } from '../axe'

// US-EXI-17 : onglet « Résultats de la sandbox » du dossier de liaison (DES-09), lecture seule.
afterEach(cleanup)
const avec = (surcharge: Partial<Services['parcours']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, parcours: { ...s.parcours, ...surcharge } }
}
const page = (id: string, o: { services?: Services; langue?: 'fr' | 'en' | 'es' } = {}) =>
  rendrePage(<DossierLiaisonPage />, {
    chemin: `/des/parcours/${id}?onglet=sandbox`, motif: '/des/parcours/:id', langue: o.langue, services: o.services,
    session: sessionTest(PROFILS.ROLE_SNA, ['ROLE_SNA']),
  })

describe('Résultats de la sandbox (US-EXI-17)', () => {
  // Onglet complet (vue graphique, répartition, exécutions paginées, contrôle axe) : délai allongé sous la charge du poste.
  it('critère 1 : verdicts natif et normalisé, version de la table, taux avec numérateur, dénominateur et version de formule ; mention permanente, aucune action', async () => {
    const { container } = page('LIA-2026-022')
    expect(await screen.findByRole('tab', { name: 'Résultats de la sandbox' })).toBeTruthy()
    expect(await screen.findByText('Repris de la sandbox, jamais recalculé')).toBeTruthy()
    expect(await screen.findByText('Campagne CMP-LIA-2026-022/CAR')).toBeTruthy()
    const taux = screen.getByText('Taux par service').closest('section, .gm-card, div') as HTMLElement
    expect(within(taux).getByText('Version de formule')).toBeTruthy()
    expect(screen.getByText('Version de la table des verdicts')).toBeTruthy()
    expect(screen.getAllByText('Campagne entière').length).toBe(2)
    expect(screen.getAllByText('100.00 %').length).toBeGreaterThan(0)
    // Vue graphique : jauges d'avancement et de réussite (numérateur sur dénominateur), verdicts, réussite par service.
    const graphes = screen.getByLabelText('Vue graphique de la campagne')
    expect(within(graphes).getByRole('img', { name: 'Avancement de la campagne : 100%' })).toBeTruthy()
    expect(within(graphes).getByText('45 sur 45')).toBeTruthy()
    expect(within(graphes).getByRole('img', { name: /Réussite de la campagne : 97\.7%/ })).toBeTruthy()
    expect(within(graphes).getByText('Non encore jugés')).toBeTruthy()
    expect(within(graphes).getByText('Réussite par service')).toBeTruthy()
    // Répartition comptée par MS-HOMOLOGATION : verdicts par cas, chronologie par jour du fuseau du GIMAC.
    expect(await screen.findByText('Verdicts par cas')).toBeTruthy()
    expect(screen.getByText('6 cas les plus défavorables sur 6')).toBeTruthy()
    expect(screen.getByText('Exécutions par jour (Africa/Douala)')).toBeTruthy()
    // Exécutions paginées par le service : 45 au total, 25 par page.
    expect(await screen.findByText('EXE-000003')).toBeTruthy()
    expect(screen.getAllByText('Favorable').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Défavorable').length).toBeGreaterThan(0)
    expect(screen.getAllByText('PASSED').length).toBeGreaterThan(0)
    // Pièces et empreinte copiable.
    expect(screen.getByText('Bilan de certification')).toBeTruthy()
    expect(screen.getByText('Déjà conservée')).toBeTruthy()
    // Lecture seule : aucun bouton d'action dans l'onglet hors pagination, copie d'empreinte, onglets et bascule
    // graphique ou tableau des cartes de graphe (aucun export sans action).
    const section = container.querySelector('section[aria-label="Résultats de la sandbox"]') as HTMLElement
    const boutons = within(section).queryAllByRole('button').map((b) => b.getAttribute('aria-label') ?? b.textContent ?? '')
    expect(boutons.filter((b) => !/Précédente|Suivante|Copier|copier|^Graphique$|^Tableau$/.test(b))).toEqual([])
    await axeSansViolation(container)
  }, 45_000)

  it('pagination serveur des exécutions : page suivante, taille de page', async () => {
    const appels: [number, number][] = []
    const s = servicesSimules({ latenceMs: 0 })
    page('LIA-2026-022', { services: avec({ executionsSandbox: (id, c, p, t) => { appels.push([p, t]); return s.parcours.executionsSandbox(id, c, p, t) } }) })
    expect(await screen.findByText('EXE-000003')).toBeTruthy()
    const executions = screen.getByText('Exécutions retenues').closest('.gm-card') as HTMLElement
    fireEvent.click(within(executions).getByRole('button', { name: 'Suivante' }))
    await waitFor(() => expect(appels).toContainEqual([1, 25]))
    fireEvent.change(await within(executions).findByRole('combobox'), { target: { value: '50' } })
    await waitFor(() => expect(appels).toContainEqual([0, 50]))
  })

  it('critère 2 : exécutions de scénarios inconnus signalées parmi les scénarios non rapprochés', async () => {
    page('LIA-2026-022')
    expect(await screen.findByRole('heading', { name: 'Scénarios non rapprochés' })).toBeTruthy()
    expect((await screen.findAllByText('SCN-INCONNU-1')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('À qualifier').length).toBe(2)
    expect(screen.getAllByText('Non rapprochée').length).toBeGreaterThan(0)
  })

  it('critère 3 : sandbox indisponible, résultats déjà repris affichés avec la date de la dernière relève réussie', async () => {
    page('LIA-2026-027')
    expect(await screen.findByText('Dernière relève en échec')).toBeTruthy()
    expect(screen.getByText(/SBX-LECTURE répond 503/)).toBeTruthy()
    expect(screen.getByText(/dernière relève réussie, le/)).toBeTruthy()
    expect(screen.getByText('Campagne CMP-LIA-2026-027/CAR')).toBeTruthy()
  })

  it('aucune campagne avant les essais ; relève en échec sans réussite ni motif ; signalement de collision ; erreur de chargement', async () => {
    page('LIA-2026-030')
    expect(await screen.findByText('Aucune campagne reprise pour cette liaison')).toBeTruthy()
    expect(screen.getByText('Aucune relève réussie')).toBeTruthy()
    cleanup()
    const vide: ResultatsSandbox = {
      liaisonId: 'LIA-2026-022', mention: 'REPRIS_NON_RECALCULE', derniereReleveReussie: null, dernierEchec: '2026-10-22T09:10:00Z', motifEchec: null, enEchec: true,
      campagnes: [{ campagneId: 'C-9', code: null, etat: 'SIGNALEE', interfaceTest: null, services: [], versionBilan: null, versionTable: null, signalement: 'COLLISION_CODE_PARTICIPANT', taux: [], executions: 0, nonRapprochees: 0, pieces: [], releveLe: null }],
      scenariosNonRapproches: [],
    }
    page('LIA-2026-022', { services: avec({ resultatsSandbox: async () => vide, executionsSandbox: async () => ({ contenu: [], total: 0, page: 0, taille: 25 }), repartitionSandbox: async () => ({ fuseau: 'Africa/Douala', parCas: [{ identifiantCas: null, executions: 1, favorables: 0, defavorables: 0, nonExecutes: 0, enAttente: 1 }], parJour: [] }) }) })
    expect(await screen.findByText(/motif non communiqué/)).toBeTruthy()
    expect(screen.getByText('Campagne remplacée par un signalement')).toBeTruthy()
    expect(screen.getByText('Aucun taux restitué')).toBeTruthy()
    expect(screen.getByText('Aucun taux par service à représenter')).toBeTruthy()
    expect((await screen.findAllByText('Sans cas')).length).toBeGreaterThan(0)
    expect(screen.getByText('Aucune pièce reprise')).toBeTruthy()
    expect(await screen.findByText('Aucune exécution reprise')).toBeTruthy()
    expect(screen.getByText('Tous les scénarios sont rapprochés du référentiel d’opérations')).toBeTruthy()
    cleanup()
    page('LIA-2026-022', { services: avec({ resultatsSandbox: async () => { throw new ApiError(503, 'HOM_INDISPONIBLE', 'Indisponible', 'RQ-SBX') } }) })
    expect(await screen.findByText('RQ-SBX')).toBeTruthy()
  })

  it('copie de l’empreinte d’une pièce ; choix d’une autre campagne', async () => {
    const ecrire = vi.fn(async () => undefined)
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: ecrire }, configurable: true })
    const s = servicesSimules({ latenceMs: 0 })
    page('LIA-2026-022', {
      services: avec({
        resultatsSandbox: async (id) => {
          const r = await s.parcours.resultatsSandbox(id)
          return { ...r, campagnes: [...r.campagnes, { ...r.campagnes[0], campagneId: 'C-2', code: 'CMP-SECONDE', etat: 'EN_COURS' }] }
        },
      }),
    })
    const copies = await screen.findAllByRole('button', { name: /copier/i })
    fireEvent.click(copies[0])
    await waitFor(() => expect(ecrire).toHaveBeenCalled())
    fireEvent.click(screen.getByText('CMP-SECONDE'))
    expect(await screen.findByText('Campagne CMP-SECONDE')).toBeTruthy()
  })

  it('valeurs hors catalogue : état, statut de pièce et verdict inconnus, verdict absent, date illisible, campagne non rattachée ; exécutions rechargées après erreur ; copie refusée', async () => {
    Object.defineProperty(navigator, 'clipboard', { value: { writeText: vi.fn(async () => { throw new Error('refus') }) }, configurable: true })
    const s = servicesSimules({ latenceMs: 0 })
    const base = await s.parcours.resultatsSandbox('LIA-2026-022')
    const c0 = base.campagnes[0]
    const etrange: ResultatsSandbox = {
      ...base, enEchec: true, dernierEchec: null, motifEchec: 'SANDBOX_INDISPONIBLE',
      campagnes: [{ ...c0, etat: 'ETAT_INCONNU', pieces: c0.pieces.map((p) => ({ ...p, statut: 'AUTRE' })) }],
      scenariosNonRapproches: [{ id: 's-1', idScenario: 'SCN-X', campagneId: null, qualification: 'INCONNU', dateDetection: '2026-10-22T08:00:00Z' }],
    }
    const page1 = await s.parcours.executionsSandbox('LIA-2026-022', c0.campagneId, 0, 25)
    let appels = 0
    page('LIA-2026-022', {
      services: avec({
        resultatsSandbox: async () => etrange,
        executionsSandbox: async () => {
          appels++
          if (appels === 1) throw new ApiError(503, 'HOM_INDISPONIBLE', 'Indisponible', 'RQ-EXE')
          return { ...page1, contenu: page1.contenu.map((e, i) => ({ ...e, verdictNormalise: i === 0 ? null : 'bizarre' })) }
        },
      }),
    })
    expect(await screen.findByText('ETAT_INCONNU')).toBeTruthy()
    expect(screen.getAllByText('AUTRE').length).toBeGreaterThan(0)
    expect(screen.getByText('SCN-X')).toBeTruthy()
    fireEvent.click(await screen.findByRole('button', { name: 'Réessayer' }))
    expect((await screen.findAllByText('bizarre')).length).toBeGreaterThan(0)
    fireEvent.click((await screen.findAllByRole('button', { name: /copier/i }))[0])
    cleanup()
    page('LIA-2026-022', { services: avec({ resultatsSandbox: async () => ({ ...base, enEchec: true, dernierEchec: 'pas-une-date' }) }) })
    expect(await screen.findByText(/pas-une-date/)).toBeTruthy()
  })

  it('libellés en anglais et en espagnol', async () => {
    page('LIA-2026-022', { langue: 'en' })
    expect(await screen.findByText('Taken from the sandbox, never recalculated')).toBeTruthy()
    cleanup()
    page('LIA-2026-022', { langue: 'es' })
    expect(await screen.findByText('Tomado del sandbox, nunca recalculado')).toBeTruthy()
  })
})
