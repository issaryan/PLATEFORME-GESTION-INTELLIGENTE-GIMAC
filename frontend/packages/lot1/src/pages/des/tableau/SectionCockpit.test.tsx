import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { Route, Routes, useLocation } from 'react-router'
import axe from 'axe-core'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { SectionCockpit } from './SectionCockpit'

// Cockpit de pilotage (CR §7.3, annexe B ; D-59) : quatre compteurs, répartition par étape, deux indicateurs
// de performance et les six questions, chiffres repris du service, liens vers les listes filtrées.

afterEach(cleanup)

function Lieu() {
  const l = useLocation()
  return <output data-testid="lieu">{l.pathname + l.search}</output>
}
const ouvrir = (o: { services?: Services; langue?: Langue; theme?: 'light' | 'dark' } = {}) =>
  rendrePage(
    <Routes>
      <Route path="/des/tableau-de-bord" element={<SectionCockpit />} />
      <Route path="*" element={<Lieu />} />
    </Routes>,
    { chemin: '/des/tableau-de-bord', motif: '*', services: o.services ?? servicesSimules({ latenceMs: 0 }), langue: o.langue, theme: o.theme },
  )

describe('Cockpit de pilotage', () => {
  it('quatre compteurs, indicateurs de performance et répartition par étape', async () => {
    ouvrir()
    const compteurs = await screen.findByLabelText('Compteurs d\'alerte')
    for (const l of ['Projets actifs', 'Projets bloqués', 'Tâches en retard', 'Échéances proches']) expect(within(compteurs).getByText(l)).toBeTruthy()
    expect(screen.getByText('Délai moyen de certification')).toBeTruthy()
    expect(screen.getByText('46')).toBeTruthy()
    expect(screen.getByText('78')).toBeTruthy()
    expect(screen.getByText('Répartition des projets par étape')).toBeTruthy()
    expect(screen.getByRole('link', { name: /^Cadrage et périmètre \(\d+\)$/ }).getAttribute('href')).toBe('/des/parcours?etape=CADRAGE')
  })

  it('les six questions de l\'annexe B, avec leurs réponses', async () => {
    ouvrir()
    await screen.findByText('Les six questions du pilotage')
    for (const q of [
      'Combien de participants sont en certification ?', 'Quels services sont les plus demandés ?', 'Quels services engendrent les plus longs délais ?',
      'Quel est le délai moyen entre l\'adhésion et la production ?', 'Quels projets sont bloqués, et depuis quand ?', 'Combien de lancements par service sur douze mois ?',
    ]) expect(screen.getByRole('heading', { name: q })).toBeTruthy()
    expect(screen.getByText('174')).toBeTruthy()
    expect(screen.getAllByText('Rémittence entrante').length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'LIA-2026-030' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-030')
    expect(screen.getByText(/Élément de sécurité du participant à renouveler/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Voir les liaisons bloquées' }).getAttribute('href')).toBe('/des/parcours?bloquees=1')
    expect(screen.getByRole('link', { name: /^Tests : \d+ liaisons?$/ }).getAttribute('href')).toBe('/des/parcours?etape=TESTS')
  })

  it('un compteur mène à la liste filtrée', async () => {
    ouvrir()
    const compteurs = await screen.findByLabelText('Compteurs d\'alerte')
    fireEvent.click(within(compteurs).getByText('Projets bloqués').closest('[role="button"]') as HTMLElement)
    expect((await screen.findByTestId('lieu')).textContent).toBe('/des/parcours?bloquees=1')
  })

  it('indicateurs non calculables (n/c) et aucun projet bloqué', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const services: Services = {
      ...s,
      parcours: {
        ...s.parcours,
        pilotage: () => s.parcours.pilotage().then((p) => ({ ...p, projetsBloques: 0, delaiMoyenCertificationJours: null, tauxRespectDelais: null, questions: { ...p.questions, delaiMoyenAdhesionProductionJours: null, projetsBloques: [], servicesPlusLongs: [] } })),
      },
    }
    ouvrir({ services })
    await screen.findByText('Les six questions du pilotage')
    expect(screen.getAllByText('n/c')).toHaveLength(3)
    expect(screen.getByText('Aucun projet bloqué.')).toBeTruthy()
    expect(screen.getByText('aucun blocage')).toBeTruthy()
    expect(screen.getByText('Aucune donnée.')).toBeTruthy()
  })

  it('erreur : identifiant de corrélation et réessai', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    ouvrir({ services: { ...s, parcours: { ...s.parcours, pilotage: () => Promise.reject(new ApiError(503, 'HOM_INDISPONIBLE', 'Indisponible', 'cid-77')) } } })
    const alerte = await screen.findByRole('alert')
    expect(alerte.textContent).toContain('cid-77')
  })

  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    ouvrir({ langue })
    await screen.findByText(langue === 'en' ? 'The six oversight questions' : 'Las seis preguntas del pilotaje')
    expect(document.body.textContent).not.toMatch(/\b(cockpit|parcours)\.[a-zA-Z]/)
  })

  it.each(['light', 'dark'] as const)('axe sans violation (thème %s)', async (theme) => {
    const { container } = ouvrir({ theme })
    await screen.findByText('Les six questions du pilotage')
    const res = await axe.run(container, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] }, rules: { 'color-contrast': { enabled: false }, region: { enabled: false } } })
    expect(res.violations.map((v) => `${v.id}: ${v.nodes[0]?.html.slice(0, 120)}`)).toEqual([])
  })
})
