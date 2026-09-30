// Internationalisation des composants : trois catalogues aux clés identiques, rendus sans texte
// français résiduel en anglais et en espagnol, français par défaut sans fournisseur.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { DataTable, DateField, Drawer, Hash, LangueProvider, Pagination, QuitusCircuit, type LangueDS } from '../src'
import { LIBELLES } from '../src/i18n/libelles'

afterEach(cleanup)

const enLangue = (langue: LangueDS, el: ReactNode) => render(<LangueProvider langue={langue}>{el}</LangueProvider>)
const composants = (
  <>
    <Pagination total={1284} page={1} pageSize={25} onPageSize={() => undefined} />
    <Hash value="3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f" />
    <Drawer inline type="Ticket" reference="TCK-000318" status="info" statusLabel="x" onClose={() => {}} />
    <QuitusCircuit quitus={{ tech: { state: 'signed', at: '25/09/2026 16:10' }, sec: { state: 'pending' } }} />
    <DateField name="d" label="Date" value="28/09/2026" open onChange={() => {}} />
  </>
)
const FRANCAIS = ['Lignes par page', 'Précédente', 'Suivante', 'éléments', "Copier l'empreinte", 'Ouvrir en pleine page', 'Chronologie', 'Pièces', 'Quitus technique', 'Signataire', 'signé', 'Mois précédent', 'Fuseau', 'Septembre', "aujourd'hui", 'JJ/MM/AAAA']

describe('catalogues de libellés', () => {
  it('les trois langues ont exactement les mêmes clés, toutes renseignées', () => {
    const fr = Object.keys(LIBELLES.fr).sort()
    expect(Object.keys(LIBELLES.en).sort()).toEqual(fr)
    expect(Object.keys(LIBELLES.es).sort()).toEqual(fr)
    for (const l of ['fr', 'en', 'es'] as const) for (const k of fr) expect(LIBELLES[l][k as keyof typeof LIBELLES.fr].trim(), `${l} ${k}`).not.toBe('')
  })
  it('les variables d\'interpolation sont les mêmes dans les trois langues', () => {
    const vars = (s: string) => (s.match(/\{\w+\}/g) || []).sort().join()
    for (const k of Object.keys(LIBELLES.fr) as (keyof typeof LIBELLES.fr)[]) {
      expect(vars(LIBELLES.en[k]), k).toBe(vars(LIBELLES.fr[k]))
      expect(vars(LIBELLES.es[k]), k).toBe(vars(LIBELLES.fr[k]))
    }
  })
})

describe('rendu par langue', () => {
  it('français par défaut, sans fournisseur', () => {
    const { container } = render(composants)
    expect(container.textContent).toContain('Lignes par page')
    expect(container.textContent).toContain('Page 1 sur 52')
  })
  it.each(['en', 'es'] as const)('%s : aucun libellé français résiduel', (langue) => {
    const { container } = enLangue(langue, composants)
    const texte = container.textContent + ' ' + [...container.querySelectorAll('[aria-label],[title]')].map((e) => (e.getAttribute('aria-label') || '') + ' ' + (e.getAttribute('title') || '')).join(' ')
    for (const f of FRANCAIS) expect(texte, f).not.toContain(f)
  })
  it('anglais et espagnol traduisent effectivement', () => {
    expect(enLangue('en', composants).container.textContent).toContain('Rows per page')
    cleanup()
    expect(enLangue('es', composants).container.textContent).toContain('Filas por página')
  })
})

describe('pagination', () => {
  it('sans choix de taille, pas de sélecteur ; la taille affichée est celle appliquée', () => {
    const { container, rerender } = render(<Pagination total={16} page={1} pageSize={10} />)
    expect(container.querySelector('select')).toBeNull()
    expect(container.textContent).toContain('Page 1 sur 2')
    rerender(<Pagination total={16} page={1} pageSize={10} onPageSize={() => undefined} />)
    expect((container.querySelector('select') as HTMLSelectElement).value).toBe('10')
  })
})

describe('DataTable : pagination visible sur toute liste', () => {
  const colonnes = [{ key: 'n', header: 'N' }]
  const lignes = Array.from({ length: 23 }, (_, i) => ({ n: `L${i + 1}` }))
  it('liste courte : barre de pagination affichée ; liste longue : découpée et navigable', async () => {
    const { container, rerender } = render(<DataTable columns={colonnes} rows={lignes.slice(0, 3)} />)
    expect(container.textContent).toContain('Page 1 sur 1 · 3 éléments')
    rerender(<DataTable columns={colonnes} rows={lignes} />)
    expect(container.querySelectorAll('tbody tr')).toHaveLength(10)
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    expect(container.textContent).toContain('L11')
    fireEvent.change(screen.getByLabelText('Taille de page'), { target: { value: '25' } })
    expect(container.querySelectorAll('tbody tr')).toHaveLength(23)
  })
  it('tableau de synthèse : pagination retirée à la demande', () => {
    const { container } = render(<DataTable columns={colonnes} rows={lignes.slice(0, 3)} pagination={false} />)
    expect(container.querySelector('.gm-pagination')).toBeNull()
  })
})
