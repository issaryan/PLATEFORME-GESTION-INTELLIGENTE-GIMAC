// Finition D-66 (sobriété DS-GIMAC-01 §0.4, §5.3, §7.3) : indicateurs sans pictogramme décoratif, statut
// par pictogramme et libellé ; réglages en lecture ; note d'aide ; liste de liens paginée ; préférences
// de la barre supérieure en menu surgissant ancré ; horodatages au format local.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { LinkList, Note, SettingsGrid, StatCard, TopBar } from '../src'
import { renderField } from '../src/crud/outils'

afterEach(cleanup)

describe('StatCard sobre', () => {
  it('libellé, valeur, unité ; statut par pictogramme réservé et libellé ; tendance, seuil, contexte', () => {
    const { container } = render(<StatCard label="Respect des engagements" value="97,4" unit="%" status="crit" statusLabel="seuil franchi"
      trend={{ direction: 'down', delta: '-1,2 pt' }} threshold="95 %" context="sur 30 jours" />)
    expect(container.querySelector('.gm-stat-label')?.textContent).toBe('Respect des engagements')
    expect(container.querySelector('.gm-stat-value')?.textContent).toBe('97,4%')
    const etat = container.querySelector('.gm-stat-etat-crit')!
    expect(etat.textContent).toBe('seuil franchi')
    expect(etat.querySelector('svg')).toBeTruthy()
    expect(container.querySelector('.gm-stat-tendance')?.textContent).toBe('-1,2 pt')
    expect(screen.getByText('sur 30 jours')).toBeTruthy()
    // Ni pictogramme décoratif, ni flèche de renvoi, ni pastille.
    expect(container.querySelector('.gm-stat-icone, .gm-stat-fleche, .gm-pastille')).toBeNull()
    expect(container.querySelectorAll('svg')).toHaveLength(2)
  })
  it('statut neutre : libellé seul ; sans statut ni contexte : pas de ligne de contexte', () => {
    const { container, rerender } = render(<StatCard label="Brouillons" value={2} status="neutral" statusLabel="brouillon" />)
    expect(container.querySelector('.gm-stat-etat svg')).toBeNull()
    expect(container.querySelector('.gm-stat-etat')?.textContent).toBe('brouillon')
    rerender(<StatCard label="Comptes" value={13} />)
    expect(container.querySelector('.gm-stat-meta')).toBeNull()
  })
  it('carte cliquable : Entrée et Espace', () => {
    const clic = vi.fn()
    render(<StatCard label="Comptes" value={13} onClick={clic} />)
    const carte = screen.getByRole('button')
    fireEvent.keyDown(carte, { key: 'Enter' })
    fireEvent.keyDown(carte, { key: ' ' })
    fireEvent.keyDown(carte, { key: 'a' })
    expect(clic).toHaveBeenCalledTimes(2)
  })
})

describe('SettingsGrid', () => {
  it('libellé, valeur, unité, aide ; état booléen par pictogramme de statut', () => {
    const { container } = render(<SettingsGrid label="Mots de passe" items={[
      { key: 'l', label: 'Longueur minimale', value: 14, hint: 'De 12 à 128.' },
      { key: 'e', label: 'Expiration', value: 90, unit: 'jours' },
      { key: 'i', label: "Interdire l'identifiant", value: 'activée', status: 'ok' },
      { key: 'n', label: 'Brouillon', value: 'non', status: 'neutral' },
    ]} />)
    const dl = screen.getByLabelText('Mots de passe')
    expect(dl.tagName).toBe('DL')
    expect(within(dl).getByText('De 12 à 128.').className).toBe('gm-reglage-aide')
    expect(within(dl).getByText('jours').className).toBe('gm-reglage-unite')
    expect(container.querySelector('.gm-reglage-etat.gm-stat-etat-ok svg')).toBeTruthy()
    expect(container.querySelectorAll('.gm-reglage-etat')).toHaveLength(1)
  })
})

describe('Note', () => {
  it('cartouche avec pictogramme info par défaut, titre ; sans pictogramme sur demande', () => {
    const { container, rerender } = render(<Note title="Règle">Texte de la note</Note>)
    expect(container.querySelector('.gm-note svg')).toBeTruthy()
    expect(screen.getByText('Règle')).toBeTruthy()
    expect(screen.getByText('Texte de la note')).toBeTruthy()
    rerender(<Note icon={null}>Sans pictogramme</Note>)
    expect(container.querySelector('.gm-note svg')).toBeNull()
  })
})

const liens = (n: number) => Array.from({ length: n }, (_, i) => ({ key: String(i), href: `#/${i}`, label: `Dossier ${i + 1}` }))

describe('LinkList paginée', () => {
  it('côté client : 5 lignes par page, pagination visible, page suivante', () => {
    render(<LinkList label="Mes dossiers" items={liens(7)} />)
    const liste = screen.getByRole('list', { name: 'Mes dossiers' })
    expect(within(liste).getAllByRole('link')).toHaveLength(5)
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    expect(within(screen.getByRole('list', { name: 'Mes dossiers' })).getAllByRole('link').map((a) => a.textContent)).toEqual(['Dossier 6', 'Dossier 7'])
  })
  it('côté serveur : page et total fournis, changement de page remonté', () => {
    const onPage = vi.fn()
    render(<LinkList label="Mes dossiers" items={liens(5)} page={1} total={12} onPage={onPage} />)
    expect(screen.getByText('Page 1 sur 3 · 12 éléments')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    expect(onPage).toHaveBeenCalledWith(2)
  })
  it('raccourcis sans pagination ; liste vide expliquée', () => {
    const { rerender } = render(<LinkList label="Raccourcis" items={liens(7)} pagination={false} />)
    expect(screen.getAllByRole('link')).toHaveLength(7)
    expect(screen.queryByRole('button', { name: 'Suivante' })).toBeNull()
    rerender(<LinkList label="Raccourcis" items={[]} empty="Aucun dossier." />)
    expect(screen.getByText('Aucun dossier.')).toBeTruthy()
  })
})

describe('TopBar : préférences en menu surgissant ancré (D-66)', () => {
  it('ouverture au globe, focus sur le premier contrôle, Échap ferme et rend le focus', () => {
    const onLangue = vi.fn()
    render(<TopBar onLangue={onLangue} preferences={<select aria-label="Langue"><option>fr</option></select>} />)
    const globe = screen.getByRole('button', { name: /Langue/ })
    expect(globe.getAttribute('aria-haspopup')).toBe('dialog')
    expect(globe.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(globe)
    const menu = screen.getByRole('dialog', { name: "Préférences d'affichage" })
    expect(menu.className).toContain('gm-topbar-pop')
    expect(globe.getAttribute('aria-expanded')).toBe('true')
    expect(document.activeElement).toBe(within(menu).getByRole('combobox'))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(document.activeElement).toBe(globe)
    expect(onLangue).toHaveBeenCalledTimes(1)
  })
  it('un clic à l’extérieur ferme ; un clic dans le menu le garde ouvert ; aucun bandeau d’environnement', () => {
    const { container } = render(<TopBar env="prod" preferences={<button type="button">Thème</button>} />)
    fireEvent.click(screen.getByRole('button', { name: /Langue/ }))
    fireEvent.mouseDown(screen.getByRole('button', { name: 'Thème' }))
    expect(screen.getByRole('dialog')).toBeTruthy()
    fireEvent.mouseDown(document.body)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(container.textContent).not.toMatch(/PRODUCTION/i)
  })
})

describe('Horodatages des fiches (§4.3)', () => {
  const desc = { key: 'x', label: { singular: 'x', plural: 'x', gender: 'm' as const }, idField: 'id', titleField: 'id', endpoints: { list: '', get: '' }, fields: [], actions: [], permissions: { list: '', read: '' } }
  const rendre = (value: unknown, kind: 'date' | 'datetime') => render(<>{renderField({ name: 'd', label: 'd', kind } as never, value, {} as never, desc as never, (k: string) => k)}</>).container
  it('date seule au format JJ/MM/AAAA', () => {
    expect(rendre('2026-10-01', 'date').textContent).toBe('01/10/2026')
  })
  it('horodatage en heure d’Afrique centrale (UTC+1), UTC en infobulle', () => {
    const c = rendre('2026-09-28T13:05:00Z', 'datetime')
    expect(c.textContent).toBe('28/09/2026 14:05')
    expect(c.querySelector('time')?.getAttribute('title')).toBe('28/09/2026 13:05 UTC')
    expect(c.querySelector('time')?.getAttribute('dateTime')).toBe('2026-09-28T13:05:00Z')
  })
  it('date ISO rendue en date seule ; valeur non ISO telle quelle ; vide inchangé', () => {
    expect(rendre('2026-09-28T23:30:00Z', 'date').textContent).toBe('29/09/2026')
    expect(rendre('hier', 'datetime').textContent).toBe('hier')
    expect(rendre('', 'date').textContent).toBe('')
  })
})
