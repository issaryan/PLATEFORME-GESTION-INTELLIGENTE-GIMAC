import { describe, expect, it } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import SupervisionPage from './SupervisionPage'
import { rendre, simules } from '../test/rendu'
import { ALERTES_DEMO, DISPONIBILITES_DEMO, SANTE_DEMO } from '../services/supervisionSimulee'

describe('SupervisionPage', () => {
  it('affiche la santé détaillée, les éléments, les alertes et les disponibilités', async () => {
    rendre(<SupervisionPage />)
    expect(await screen.findByRole('heading', { level: 1, name: 'Supervision de la sandbox' })).toBeTruthy()
    expect(await screen.findByText('Dégradé')).toBeTruthy()
    expect(screen.getByLabelText('Détail des états par module').textContent).toContain('SBX-CARTE')
    expect(screen.getByText('Cause externe: Non')).toBeTruthy()
    expect(screen.getByText('Délai dépassé sur la liaison de test')).toBeTruthy()
    expect(screen.getByText('Levée')).toBeTruthy()
    expect(screen.getByText('Close')).toBeTruthy()
    expect(screen.getByText('99.50 %')).toBeTruthy()
    expect(screen.getByText('100.00 %')).toBeTruthy()
    expect(screen.getAllByText('Tous').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Oui').length).toBeGreaterThan(0)
  })

  it('pagine indépendamment les trois listes, dans les deux sens', async () => {
    const alertes = Array.from({ length: 12 }, (_, i) => ({ ...ALERTES_DEMO[0], id: `a-${i}` }))
    const disponibilites = Array.from({ length: 12 }, (_, i) => ({ ...DISPONIBILITES_DEMO[0], id: `d-${i}` }))
    rendre(<SupervisionPage />, { services: simules({ alertesSandbox: alertes, disponibilitesSandbox: disponibilites }) })
    await screen.findByText('Page 1 sur 3 · 23 éléments')
    const suivantes = screen.getAllByRole('button', { name: 'Page suivante' })
    suivantes.forEach((b) => fireEvent.click(b))
    expect(await screen.findByText('Page 2 sur 3 · 23 éléments')).toBeTruthy()
    expect(await screen.findAllByText('Page 2 sur 2 · 12 éléments')).toHaveLength(2)
    const precedentes = screen.getAllByRole('button', { name: 'Page précédente' })
    precedentes.forEach((b) => fireEvent.click(b))
    expect(await screen.findByText('Page 1 sur 3 · 23 éléments')).toBeTruthy()
    expect((screen.getAllByRole('button', { name: 'Page précédente' })[0] as HTMLButtonElement).disabled).toBe(true)
    expect((screen.getAllByRole('button', { name: 'Page suivante' })[2] as HTMLButtonElement).disabled).toBe(false)
  })

  it('affiche les listes vides et les autres niveaux de santé', async () => {
    rendre(<SupervisionPage />, { services: simules({ elementsSupervises: [], alertesSandbox: [], disponibilitesSandbox: [], sante: { ...SANTE_DEMO, niveauGlobal: 'EN_MAINTENANCE', causeExterne: true } }) })
    expect(await screen.findByText('Aucun élément supervisé')).toBeTruthy()
    expect(screen.getByText('Aucune alerte')).toBeTruthy()
    expect(screen.getByText('Aucune disponibilité')).toBeTruthy()
    expect(screen.getByText('En maintenance')).toBeTruthy()
    expect(screen.getByText('Cause externe: Oui')).toBeTruthy()
    expect(screen.getAllByText('Page 1 sur 1 · 0 éléments')).toHaveLength(3)
  })

  it('propose une nouvelle tentative sur chacune des quatre lectures', async () => {
    const s = simules()
    let essais = 0
    const echoueUneFois = async <T,>(f: () => Promise<T>) => { if (essais++ < 4) throw new Error('panne'); return f() }
    const services = { ...s, supervision: {
      etatSante: () => echoueUneFois(s.supervision.etatSante),
      elements: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.elements(q)),
      alertes: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.alertes(q)),
      disponibilites: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.disponibilites(q)),
    } }
    rendre(<SupervisionPage />, { services })
    const boutons = await screen.findAllByRole('button', { name: 'Réessayer' })
    expect(boutons).toHaveLength(4)
    boutons.forEach((b) => fireEvent.click(b))
    expect(await screen.findByText('Dégradé')).toBeTruthy()
    expect(await screen.findByText('Délai dépassé sur la liaison de test')).toBeTruthy()
    expect(await screen.findByText('99.50 %')).toBeTruthy()
  })

  it('est traduit en anglais et en espagnol', async () => {
    const r = rendre(<SupervisionPage />, { langue: 'en' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Sandbox supervision' })).toBeTruthy()
    r.unmount()
    rendre(<SupervisionPage />, { langue: 'es' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Supervisión del sandbox' })).toBeTruthy()
  })
})
