import { describe, expect, it } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import SupervisionPage, { modules } from './SupervisionPage'
import { rendre, simules } from '../test/rendu'
import { ALERTES_DEMO, DISPONIBILITES_DEMO, SANTE_DEMO, SERVICES_DEMO } from '../services/supervisionSimulee'

describe('SupervisionPage', () => {
  it('affiche la santé détaillée, les éléments, les alertes et les disponibilités', async () => {
    rendre(<SupervisionPage />)
    expect(await screen.findByRole('heading', { level: 1, name: 'Supervision de la sandbox' })).toBeTruthy()
    expect((await screen.findAllByText('Dégradé')).length).toBeGreaterThan(0)
    expect(screen.getByLabelText('Détail des états par module').textContent).toContain('carte')
    expect(screen.getByText('Cause externe: Non')).toBeTruthy()
    expect(screen.getByText('Délai dépassé sur la liaison de test')).toBeTruthy()
    expect(screen.getByText('Levée')).toBeTruthy()
    expect(screen.getByText('Close')).toBeTruthy()
    expect(screen.getByText('99.50 %')).toBeTruthy()
    expect(screen.getAllByText('100.00 %').length).toBeGreaterThan(1)
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
    fireEvent.click(screen.getAllByRole('button', { name: 'Page suivante' })[0])
    expect(await screen.findByText('Page 3 sur 3 · 23 éléments')).toBeTruthy()
    expect(screen.getAllByText('Oui').length).toBeGreaterThan(1)
    fireEvent.click(screen.getAllByRole('button', { name: 'Page précédente' })[0])
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

  it('propose une nouvelle tentative sur chacune des cinq lectures', async () => {
    const s = simules()
    let essais = 0
    const echoueUneFois = async <T,>(f: () => Promise<T>) => { if (essais++ < 5) throw new Error('panne'); return f() }
    const services = { ...s, supervision: {
      etatSante: () => echoueUneFois(s.supervision.etatSante),
      elements: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.elements(q)),
      alertes: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.alertes(q)),
      disponibilites: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.disponibilites(q)),
      services: (q: { page: number; taille: number }) => echoueUneFois(() => s.supervision.services(q)),
    } }
    rendre(<SupervisionPage />, { services })
    await waitFor(() => expect(screen.getAllByRole('button', { name: 'Réessayer' })).toHaveLength(5))
    const boutons = screen.getAllByRole('button', { name: 'Réessayer' })
    boutons.forEach((b) => fireEvent.click(b))
    expect((await screen.findAllByText('Dégradé')).length).toBeGreaterThan(0)
    expect(await screen.findByText('Délai dépassé sur la liaison de test')).toBeTruthy()
    expect(await screen.findByText('99.50 %')).toBeTruthy()
    expect(await screen.findByText('sbx-trace')).toBeTruthy()
  })

  it('affiche les onze services avec leurs mesures', async () => {
    rendre(<SupervisionPage />)
    expect(await screen.findByRole('heading', { name: 'Services de la sandbox' })).toBeTruthy()
    expect(await screen.findByText('Page 1 sur 1 · 11 éléments')).toBeTruthy()
    for (const s of SERVICES_DEMO) expect(screen.getByText(s.service)).toBeTruthy()
    expect(screen.getByText('98.75 %')).toBeTruthy()
    expect(screen.getAllByText('100.00 %').length).toBeGreaterThan(9)
    expect(screen.getByText(`${SERVICES_DEMO[0].latenceMoyenneMs} ms / 180 ms`)).toBeTruthy()
    expect(screen.getByRole('img', { name: /Latence moyenne de sbx-carte/ })).toBeTruthy()
    expect(screen.getAllByText('Dégradé').length).toBeGreaterThan(1)
  })

  it('distingue un service injoignable, non mesuré ou sans valeurs', async () => {
    const vide = { ...SERVICES_DEMO[0], service: 'sbx-vide', niveau: null, joignable: false, mesureLe: null, latenceMoyenneMs: null, latenceMaxMs: null, debitParMinute: null, erreurs: null, retardKafka: null, rebuts: null, disponibilite24h: null, serie: [] }
    const injoignable = { ...SERVICES_DEMO[1], service: 'sbx-arrete', niveau: 'INDISPONIBLE' as const, joignable: false }
    rendre(<SupervisionPage />, { services: simules({ servicesSupervises: [vide, injoignable] }) })
    expect(await screen.findByText('Non mesuré')).toBeTruthy()
    expect(screen.getByText('Injoignable')).toBeTruthy()
    expect(screen.getByText('— / —')).toBeTruthy()
    expect(screen.getAllByText('—').length).toBeGreaterThan(3)
    expect(modules('pas du json')).toEqual([])
    expect(modules('[1]')).toEqual([])
    expect(modules('null')).toEqual([])
    expect(modules('3')).toEqual([])
  })

  it('montre un module au niveau inconnu sans couleur', async () => {
    rendre(<SupervisionPage />, { services: simules({ sante: { ...SANTE_DEMO, etatsModules: JSON.stringify({ carte: 'INCONNU' }) } }) })
    expect((await screen.findByLabelText('Détail des états par module')).textContent).toContain('carte')
  })

  it('signale l’absence de mesure', async () => {
    rendre(<SupervisionPage />, { services: simules({ servicesSupervises: [] }) })
    expect(await screen.findByText('Aucun service mesuré')).toBeTruthy()
  })

  it('est traduit en anglais et en espagnol', async () => {
    const r = rendre(<SupervisionPage />, { langue: 'en' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Sandbox supervision' })).toBeTruthy()
    expect(await screen.findByRole('heading', { name: 'Sandbox services' })).toBeTruthy()
    r.unmount()
    rendre(<SupervisionPage />, { langue: 'es' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Supervisión del sandbox' })).toBeTruthy()
    expect(await screen.findByRole('heading', { name: 'Servicios del sandbox' })).toBeTruthy()
  })
})
