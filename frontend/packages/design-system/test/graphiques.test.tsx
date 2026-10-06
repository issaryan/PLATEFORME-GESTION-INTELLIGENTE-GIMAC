// Graphiques de métriques (supervision, tableaux de bord) : mini-courbe, tuile de mesure, courbe temporelle et
// barres horizontales, composés à partir du système de conception (D-63) et des jetons gm-serie-*.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import type { ReactNode } from 'react'
import { BarresHorizontales, LangueProvider, SerieTemporelle, Sparkline, StatCard, type LangueDS } from '../src'
import { cheminAire, cheminCourbe, classeSerie, echelle, formatCompact, graduationsPoints, graduationsTemps, libelleGraduation, libellePeriode, libelleTemps, pasRond } from '../src/lib/graphiques'

afterEach(cleanup)

const enLangue = (langue: LangueDS, el: ReactNode) => render(<LangueProvider langue={langue}>{el}</LangueProvider>)
const T0 = Date.UTC(2026, 9, 6, 8, 0, 0)
const minute = 60_000
const serie = (valeurs: (number | null)[], pas = 5 * minute) => valeurs.map((v, i) => ({ t: T0 + i * pas, v }))

describe('outils des graphiques', () => {
  it('pas et échelle ronds, domaine imposé respecté', () => {
    expect(pasRond(0)).toBe(1)
    expect(pasRond(97)).toBe(25)
    expect(pasRond(8)).toBe(2)
    expect(pasRond(4.2)).toBe(1)
    expect(pasRond(3.9)).toBe(1)
    expect(pasRond(41)).toBe(10)
    const e = echelle([3, 47, null])
    expect(e.min).toBe(0)
    expect(e.max).toBe(50)
    expect(e.graduations).toEqual([0, 10, 20, 30, 40, 50])
    const d = echelle([95, 99], 90, 100)
    expect([d.min, d.max]).toEqual([90, 100])
    expect(echelle([]).max).toBeGreaterThan(0)
    expect(echelle([5, 5], 5, 5).max).toBe(5)
  })
  it('tracé avec trous : nouveau segment après une mesure absente ; aire par segment continu', () => {
    const pts = [{ x: 0, y: 10 }, { x: 1, y: 20 }, { x: 2, y: null }, { x: 3, y: 5 }, { x: 4, y: 6 }]
    expect(cheminCourbe(pts)).toBe('M0.0 10.0 L1.0 20.0 M3.0 5.0 L4.0 6.0')
    const aire = cheminAire([...pts, { x: 5, y: null }, { x: 6, y: 1 }], 30)
    expect(aire.match(/Z/g)?.length).toBe(2)
    expect(aire.startsWith('M0.0 30.0')).toBe(true)
  })
  it('nombres compacts et libellés temporels selon la langue', () => {
    expect(formatCompact(null)).toBe('—')
    expect(formatCompact(Number.NaN)).toBe('—')
    expect(formatCompact(12.345)).toBe('12,3')
    expect(formatCompact(1.234, 'en')).toBe('1.23')
    expect(formatCompact(250)).toBe('250')
    expect(formatCompact(12900, 'en')).toBe('12.9K')
    expect(formatCompact(7, 'xx')).toBe('7')
    expect(graduationsTemps(T0, T0).length).toBe(1)
    const g = graduationsTemps(T0, T0 + 6 * 3600e3, 5)
    expect(g.length).toBeGreaterThan(1)
    expect(g.every((x) => x >= T0 && x <= T0 + 6 * 3600e3)).toBe(true)
    expect(graduationsTemps(T0, T0 + 400 * 86400e3, 5).length).toBeGreaterThan(0)
    expect(libelleTemps(T0, 3600e3)).toMatch(/^\d{2}:\d{2}$/)
    expect(libelleTemps(T0, 7 * 86400e3)).toMatch(/^\d{2}\/\d{2}$/)
    expect([classeSerie(0), classeSerie(3), classeSerie(4)]).toEqual(['gm-serie-1', 'gm-serie-4', 'gm-serie-autre'])
  })
})

describe('Sparkline', () => {
  it('résume la série, place le point final sur la dernière mesure et accepte les trous', () => {
    const { container } = render(<Sparkline points={[12, null, 20, 34, null]} label="Processeur" unit="%" aire />)
    expect(screen.getByRole('img', { name: 'Processeur : de 12 % à 34 %' })).toBeTruthy()
    expect(container.querySelector('.gm-serie-aire')).toBeTruthy()
    expect(container.querySelector('.gm-sparkline-cadre')?.className).toContain('gm-serie-1')
    expect(container.querySelector('circle')?.getAttribute('cx')).toBe('75.00%')
  })
  it('sans mesure : étiquette explicite ; série de contexte en gris ; valeur constante centrée', () => {
    render(<Sparkline points={[null, null]} label="Mémoire" />)
    expect(screen.getByRole('img', { name: 'Mémoire : aucune mesure' })).toBeTruthy()
    cleanup()
    const { container } = render(<Sparkline points={[5]} label="Constante" serie="autre" hauteur={20} />)
    expect(container.querySelector('.gm-sparkline-cadre')?.className).toContain('gm-serie-autre')
    expect(container.querySelector('circle')?.getAttribute('cy')).toBe('10')
  })
})

describe('StatCard en tuile de mesure', () => {
  it('affiche la mini-courbe sous la valeur, étiquetée par le libellé ou serieLibelle', () => {
    render(<StatCard label="Processeur" value="34" unit="%" serie={[10, 20, 34]} />)
    expect(screen.getByRole('img', { name: 'Processeur : de 10 % à 34 %' })).toBeTruthy()
    cleanup()
    render(<StatCard label={<b>CPU</b>} value="1" serie={[1, 2]} serieLibelle="Charge" />)
    expect(screen.getByRole('img', { name: 'Charge : de 1 à 2' })).toBeTruthy()
    cleanup()
    const { container } = render(<StatCard label={<b>X</b>} value="1" serie={[]} />)
    expect(container.querySelector('.gm-stat-tendance-courbe')).toBeNull()
  })
})

describe('SerieTemporelle', () => {
  const series = [
    { nom: 'gateway', points: serie([10, 12, null, 20]) },
    { nom: 'iam', points: serie([5, 6, 7, 8]) },
  ]
  it('courbes, légende dès deux séries, graduations, seuil et points finaux', () => {
    const { container } = render(<SerieTemporelle title="Processeur par service" subtitle="Dernière heure" series={series} unit="%" seuil={80} status="ok" statusLabel="Normal" />)
    expect(screen.getByRole('region', { name: 'Processeur par service' })).toBeTruthy()
    expect(container.querySelectorAll('.gm-serie-trait').length).toBe(2)
    expect(container.querySelector('.gm-serie-aire')).toBeNull()
    expect(container.querySelector('.gm-chart-threshold')).toBeTruthy()
    const legende = container.querySelector('.gm-chart-legend') as HTMLElement
    expect(within(legende).getByText('gateway')).toBeTruthy()
    expect(within(legende).getByText('iam')).toBeTruthy()
    expect(screen.getByText('Normal')).toBeTruthy()
    expect(screen.getByText('Dernière heure')).toBeTruthy()
    // Pas de bouton d'export sans action fournie.
    expect(screen.queryByRole('button', { name: /Exporter/ })).toBeNull()
  })
  it('le seuil entre dans le domaine de l’axe (jamais hors du graphique)', () => {
    const { container } = render(<SerieTemporelle title="CPU" series={[{ nom: 'a', points: serie([20, 40]) }]} unit="%" seuil={80} />)
    const y = Number(container.querySelector('.gm-chart-threshold')?.getAttribute('y1'))
    expect(y).toBeGreaterThanOrEqual(10)
    expect(screen.getByText('80 %')).toBeTruthy()
  })
  it('série unique : aire sous la courbe, pas de légende ; domaine imposé ; format personnalisé', () => {
    const { container } = render(<SerieTemporelle title="Disponibilité" series={[{ nom: 'Disponibilité', points: serie([99, 98.5]) }]} min={90} max={100} format={(v) => v.toFixed(1) + ' %'} />)
    expect(container.querySelector('.gm-serie-aire')).toBeTruthy()
    expect(container.querySelector('.gm-chart-legend')).toBeNull()
    expect(screen.getByText('90.0 %')).toBeTruthy()
  })
  it('au-delà de quatre séries : traits gris et « Autres (n) » dans la légende', () => {
    const six = Array.from({ length: 6 }, (_, i) => ({ nom: 's' + i, points: serie([i, i + 1]) }))
    const { container } = render(<SerieTemporelle title="Mémoire" series={six} />)
    expect(container.querySelectorAll('g.gm-serie-autre').length).toBe(2)
    expect(screen.getByText('Autres (2)')).toBeTruthy()
    expect(screen.queryByText('s5', { selector: '.gm-chart-legend span' })).toBeNull()
  })
  it('réticule et infobulle au survol, listant toutes les séries ; trou rendu « — »', () => {
    const { container } = render(<SerieTemporelle title="CPU" series={series} unit="%" />)
    const zone = screen.getByTestId('zone-survol')
    const svg = container.querySelector('svg') as SVGSVGElement
    svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 640, height: 200, right: 640, bottom: 200, x: 0, y: 0, toJSON: () => ({}) })
    fireEvent.pointerMove(zone, { clientX: 400 })
    const bulle = screen.getByRole('status')
    expect(within(bulle).getByText('gateway')).toBeTruthy()
    expect(within(bulle).getByText('—')).toBeTruthy()
    expect(container.querySelector('.gm-graphique-reticule')).toBeTruthy()
    fireEvent.pointerMove(zone, { clientX: 630 })
    expect(within(screen.getByRole('status')).getByText('20 %')).toBeTruthy()
    fireEvent.pointerLeave(zone)
    expect(screen.queryByRole('status')).toBeNull()
  })
  it('clavier : focus sur la dernière mesure, flèches, début, fin, Échap', () => {
    const { container } = render(<SerieTemporelle title="CPU" series={series} unit="%" />)
    const svg = container.querySelector('svg') as SVGSVGElement
    fireEvent.focus(svg)
    expect(within(screen.getByRole('status')).getByText('20 %')).toBeTruthy()
    fireEvent.keyDown(svg, { key: 'ArrowLeft' })
    expect(within(screen.getByRole('status')).getByText('7 %')).toBeTruthy()
    fireEvent.keyDown(svg, { key: 'Home' })
    expect(within(screen.getByRole('status')).getByText('10 %')).toBeTruthy()
    fireEvent.keyDown(svg, { key: 'ArrowRight' })
    expect(within(screen.getByRole('status')).getByText('12 %')).toBeTruthy()
    fireEvent.keyDown(svg, { key: 'End' })
    expect(within(screen.getByRole('status')).getByText('8 %')).toBeTruthy()
    fireEvent.keyDown(svg, { key: 'a' })
    fireEvent.keyDown(svg, { key: 'Escape' })
    expect(screen.queryByRole('status')).toBeNull()
    fireEvent.keyDown(svg, { key: 'ArrowRight' })
    fireEvent.blur(svg)
    expect(screen.queryByRole('status')).toBeNull()
  })
  it('vue Tableau équivalente et export branché', () => {
    const exporter = vi.fn()
    enLangue('en', <SerieTemporelle title="CPU" series={series} unit="%" onExporter={exporter} actions={<a href="#d">Détail</a>} />)
    fireEvent.click(screen.getByRole('tab', { name: 'Table' }))
    const tableau = screen.getByRole('table')
    expect(within(tableau).getAllByRole('row').length).toBe(5)
    expect(within(tableau).getByText('—')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Export (controlled)' }))
    expect(exporter).toHaveBeenCalled()
    expect(screen.getByText('Détail')).toBeTruthy()
  })
  it('chargement, rechargement sans flash, période sans mesure', () => {
    render(<SerieTemporelle title="CPU" series={[]} chargement />)
    expect(screen.getByRole('status').textContent).toBe('Chargement des mesures…')
    cleanup()
    const { container } = render(<SerieTemporelle title="CPU" series={series} rechargement />)
    expect(container.querySelector('.gm-graphique-cadre')?.className).toContain('gm-rechargement')
    expect(container.querySelector('section')?.getAttribute('aria-busy')).toBe('true')
    cleanup()
    render(<SerieTemporelle title="CPU" series={[{ nom: 'a', points: serie([null, null]) }]} vide="Service arrêté sur la période." />)
    expect(screen.getByText('Service arrêté sur la période.')).toBeTruthy()
    cleanup()
    render(<SerieTemporelle title="CPU" series={[]} />)
    expect(screen.getByText('Aucune mesure sur la période.')).toBeTruthy()
  })
  it('une seule mesure : point centré, aucun survol hors données', () => {
    const { container } = render(<SerieTemporelle title="CPU" series={[{ nom: 'a', points: [{ t: T0, v: 3 }] }]} />)
    expect(container.querySelector('circle')).toBeTruthy()
    const svg = container.querySelector('svg') as SVGSVGElement
    svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 0, height: 0, right: 0, bottom: 0, x: 0, y: 0, toJSON: () => ({}) })
    fireEvent.pointerMove(screen.getByTestId('zone-survol'), { clientX: 10 })
    expect(screen.getByRole('status')).toBeTruthy()
  })
  it('largeur suivie par ResizeObserver', () => {
    let rappel: ResizeObserverCallback = () => {}
    const deconnecte = vi.fn()
    vi.stubGlobal('ResizeObserver', class {
      constructor(cb: ResizeObserverCallback) { rappel = cb }
      observe() {}
      disconnect() { deconnecte() }
    })
    const { container, unmount } = render(<SerieTemporelle title="CPU" series={series} />)
    act(() => rappel([{ contentRect: { width: 900 } } as ResizeObserverEntry], {} as ResizeObserver))
    act(() => rappel([{ contentRect: { width: 0 } } as ResizeObserverEntry], {} as ResizeObserver))
    expect(container.querySelector('svg')?.getAttribute('viewBox')).toBe('0 0 900 200')
    unmount()
    expect(deconnecte).toHaveBeenCalled()
    vi.unstubAllGlobals()
  })
})

describe('Granularité jour et mois (tableaux de bord)', () => {
  const mois = Array.from({ length: 12 }, (_, i) => new Date(2025, 10 + i, 1).getTime())
  it('libellés de point et de graduation ; graduations prises parmi les points, la dernière comprise', () => {
    expect(libellePeriode(new Date(2026, 9, 1).getTime(), 'mois', 'fr')).toBe('octobre 2026')
    expect(libellePeriode(new Date(2026, 9, 6).getTime(), 'jour', 'fr')).toMatch(/^mar\.? 06\/10$/)
    expect(libellePeriode(T0, 'instant', 'fr')).toMatch(/^06\/10/)
    expect(libelleGraduation(new Date(2026, 9, 1).getTime(), 'mois', 0, 'en')).toBe('Oct 26')
    expect(libelleGraduation(new Date(2026, 9, 6).getTime(), 'jour', 0, 'fr')).toBe('06/10')
    expect(libelleGraduation(T0, 'instant', 3600e3, 'fr')).toBe(libelleTemps(T0, 3600e3, 'fr'))
    const g = graduationsPoints(mois, 5)
    expect(g.length).toBeLessThanOrEqual(6)
    expect(g[g.length - 1]).toBe(mois[11])
    expect(graduationsPoints(mois.slice(0, 3), 5)).toEqual(mois.slice(0, 3))
  })
  it('courbe mensuelle : axe en mois, vue tableau en colonne « Mois »', () => {
    render(<SerieTemporelle title="Flux" granularite="mois" series={[{ nom: 'Contacts', points: mois.map((t, i) => ({ t, v: i })) }]} />)
    expect(screen.getByText('oct. 26')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Tableau' }))
    expect(screen.getByRole('columnheader', { name: 'Mois' })).toBeTruthy()
    expect(screen.getByText('novembre 2025')).toBeTruthy()
  })
})

describe('BarresHorizontales', () => {
  const elements = [
    { libelle: 'Chef du DES', valeur: 2 },
    { libelle: 'Chargé DDRM', valeur: 9 },
    { libelle: 'RSSI', valeur: 4 },
  ]
  it('tri décroissant, valeur au bout, largeur proportionnelle, survol par barre', () => {
    const { container } = render(<BarresHorizontales title="Comptes par profil" subtitle="Actifs" elements={elements} unit="comptes" />)
    const lignes = screen.getAllByRole('listitem')
    expect(lignes.map((l) => l.getAttribute('aria-label'))).toEqual(['Chargé DDRM : 9 comptes', 'RSSI : 4 comptes', 'Chef du DES : 2 comptes'])
    const barres = container.querySelectorAll<HTMLElement>('.gm-barres-h-barre')
    expect(barres[0].style.width).toBe('100%')
    fireEvent.pointerEnter(lignes[1])
    expect(barres[1].className).toContain('gm-actif')
    fireEvent.pointerLeave(lignes[1])
    expect(barres[1].className).not.toContain('gm-actif')
  })
  it('ordre fourni conservé, regroupement « Autres », clic sur une catégorie, vue Tableau', () => {
    const clic = vi.fn()
    const nombreux = Array.from({ length: 5 }, (_, i) => ({ libelle: 'Étape ' + i, valeur: i + 1, onClick: i === 0 ? clic : undefined }))
    render(<BarresHorizontales title="Liaisons par étape" elements={nombreux} trier={false} maximum={3} format={(v) => v + ' liaisons'} actions={<span>x</span>} />)
    const lignes = screen.getAllByRole('listitem')
    expect(lignes.length).toBe(3)
    expect(lignes[2].getAttribute('aria-label')).toBe('Autres (3) : 12 liaisons')
    fireEvent.click(screen.getByRole('button', { name: 'Étape 0' }))
    expect(clic).toHaveBeenCalled()
    fireEvent.click(screen.getByRole('tab', { name: 'Tableau' }))
    expect(within(screen.getByRole('table')).getByText('12 liaisons')).toBeTruthy()
  })
  it('sans élément : message ; valeurs nulles sans division par zéro', () => {
    render(<BarresHorizontales title="Vide" elements={[]} vide="Rien à afficher." />)
    expect(screen.getByText('Rien à afficher.')).toBeTruthy()
    cleanup()
    const { container } = render(<BarresHorizontales title="Zéro" elements={[{ cle: 'a', libelle: 'A', valeur: 0 }]} />)
    expect(container.querySelector<HTMLElement>('.gm-barres-h-barre')?.style.width).toBe('0%')
    cleanup()
    render(<BarresHorizontales title="Vide2" elements={[]} />)
    expect(screen.getByText('Aucune mesure sur la période.')).toBeTruthy()
  })
})
