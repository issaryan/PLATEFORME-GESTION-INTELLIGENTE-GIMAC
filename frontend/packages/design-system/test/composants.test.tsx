// Tests de rendu, de rôles ARIA et d'interactions clavier des composants du catalogue.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import {
  Button, Checkbox, ConfirmDialog, DataTable, Drawer, Icon, IconButton, Money, StatusBadge, Tabs, TextField, formatMoney,
} from '../src'

afterEach(cleanup)

describe('Icon', () => {
  it('est décoratif sans libellé et nommé avec libellé', () => {
    const { container } = render(<><Icon name="lock" /><Icon name="lock" label="Scellé" /></>)
    const [deco, nomme] = container.querySelectorAll('svg')
    expect(deco.getAttribute('aria-hidden')).toBe('true')
    expect(nomme.getAttribute('role')).toBe('img')
    expect(nomme.getAttribute('aria-label')).toBe('Scellé')
  })
})

describe('Button', () => {
  it('est un bouton natif portant sa variante et sa taille', () => {
    render(<Button variant="primary" size="sm">Clore l'opération</Button>)
    const b = screen.getByRole('button', { name: "Clore l'opération" })
    expect(b.className).toBe('gm-btn gm-btn-primary gm-btn-sm')
    expect(b.getAttribute('type')).toBe('button')
  })
  it('annonce le chargement et explique la désactivation', () => {
    render(<><Button loading>Signer le quitus</Button><Button disabled disabledReason="Validateur distinct requis">Clore</Button></>)
    expect(screen.getByRole('button', { name: 'Signer le quitus' }).getAttribute('aria-busy')).toBe('true')
    const d = screen.getByRole('button', { name: 'Clore' }) as HTMLButtonElement
    expect(d.disabled).toBe(true)
    expect(d.title).toBe('Validateur distinct requis')
  })
  it('IconButton porte son libellé en nom accessible', () => {
    const clic = vi.fn()
    render(<IconButton icon="x" label="Fermer" onClick={clic} />)
    fireEvent.click(screen.getByRole('button', { name: 'Fermer' }))
    expect(clic).toHaveBeenCalledOnce()
  })
})

describe('StatusBadge', () => {
  it.each([
    ['ok', true], ['warn', true], ['crit', true], ['info', true], ['neutral', false],
  ] as const)('statut %s : libellé et pictogramme réservé', (status, avecPicto) => {
    const { container } = render(<StatusBadge status={status} label="libellé" />)
    expect(container.textContent).toBe('libellé')
    expect(!!container.querySelector('svg')).toBe(avecPicto)
    expect(container.firstElementChild?.className).toContain('gm-badge-' + status)
  })
})

describe('TextField', () => {
  it('lie l\'erreur au champ et la signale', () => {
    render(<TextField name="code" label="Code participant" value="" onChange={() => {}} error="Le code est obligatoire." required />)
    const champ = screen.getByLabelText(/Code participant/)
    expect(champ.getAttribute('aria-invalid')).toBe('true')
    expect(champ.getAttribute('aria-describedby')).toBe('code-error')
    expect(screen.getByRole('alert').textContent).toContain('Le code est obligatoire.')
  })
  it('remonte la saisie', () => {
    const change = vi.fn()
    render(<TextField name="sigle" label="Sigle" value="" onChange={change} />)
    fireEvent.change(screen.getByLabelText('Sigle'), { target: { value: 'EPA' } })
    expect(change).toHaveBeenCalledWith('EPA')
  })
})

describe('Checkbox', () => {
  it('remonte un booléen', () => {
    const change = vi.fn()
    render(<Checkbox label="Je confirme" checked={false} onChange={change} />)
    fireEvent.click(screen.getByLabelText('Je confirme'))
    expect(change).toHaveBeenCalledWith(true)
  })
})

describe('Tabs', () => {
  it('se parcourt aux flèches', () => {
    const change = vi.fn()
    render(<Tabs items={[{ key: 'a', label: 'Détail' }, { key: 'b', label: 'Chronologie' }]} onChange={change} />)
    const [a, b] = screen.getAllByRole('tab')
    expect(a.getAttribute('aria-selected')).toBe('true')
    fireEvent.keyDown(a, { key: 'ArrowRight' })
    expect(change).toHaveBeenCalledWith('b')
    expect(b.getAttribute('aria-selected')).toBe('true')
    fireEvent.keyDown(b, { key: 'ArrowRight' })
    expect(change).toHaveBeenLastCalledWith('a')
  })
})

describe('Money et formatMoney', () => {
  it('sépare les milliers par une espace fine et place la devise après', () => {
    expect(formatMoney(1250000)).toBe('1 250 000')
    const { container } = render(<Money amount={1250000} />)
    expect(container.textContent).toBe('1 250 000 XAF')
  })
})

describe('DataTable : les quatre états', () => {
  const colonnes = [{ key: 'code', header: 'Code', type: 'mono' as const }, { key: 'nom', header: 'Dénomination' }]
  const lignes = [{ code: 'INST-0042', nom: 'Établissement de paiement A' }]
  it('chargement : squelettes', () => {
    const { container } = render(<DataTable columns={colonnes} rows={[] as typeof lignes} rowKey={(r) => String(r.code)} state="loading" />)
    expect(container.querySelectorAll('.gm-skeleton').length).toBeGreaterThan(0)
  })
  it('vide : état vide avec cause', () => {
    render(<DataTable columns={colonnes} rows={[] as typeof lignes} rowKey={(r) => String(r.code)} state="empty" emptyText="Aucune institution" />)
    expect(screen.getByText('Aucune institution')).toBeTruthy()
  })
  it('erreur : identifiant de corrélation et réessai', () => {
    const reessai = vi.fn()
    render(<DataTable columns={colonnes} rows={[] as typeof lignes} rowKey={(r) => String(r.code)} state="error" error={{ message: 'Service indisponible.', correlationId: 'RQ-2026-0928-14' }} onRetry={reessai} />)
    expect(screen.getByRole('alert').textContent).toContain('RQ-2026-0928-14')
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(reessai).toHaveBeenCalledOnce()
  })
  it('nominal : lignes, en-têtes de colonne et pagination', () => {
    render(<DataTable columns={colonnes} rows={lignes} rowKey={(r) => String(r.code)} state="ok" total={1284} page={1} pageSize={25} />)
    expect(screen.getAllByRole('columnheader').map((c) => c.textContent)).toEqual(['Code', 'Dénomination'])
    expect(screen.getByText('INST-0042').className).toBe('gm-mono')
    expect(screen.getByText(/Page 1 sur 52/)).toBeTruthy()
  })
})

describe('ConfirmDialog', () => {
  it('bloque la confirmation tant que motif et case manquent', () => {
    const confirmer = vi.fn()
    render(<ConfirmDialog inline title="Signaler une non-conformité" confirmLabel="Signaler la non-conformité" reason irreversible onConfirm={confirmer} onClose={() => {}} />)
    const bouton = screen.getByRole('button', { name: 'Signaler la non-conformité' }) as HTMLButtonElement
    expect(screen.getByRole('alertdialog')).toBeTruthy()
    expect(bouton.disabled).toBe(true)
    fireEvent.change(screen.getByLabelText(/Motif/), { target: { value: 'Pièce légale expirée depuis 30 jours' } })
    expect(bouton.disabled).toBe(true)
    fireEvent.click(screen.getByRole('checkbox'))
    expect(bouton.disabled).toBe(false)
    fireEvent.click(bouton)
    expect(confirmer).toHaveBeenCalledOnce()
  })
})

describe('Drawer', () => {
  it('est un dialogue modal qui se ferme à Échap', () => {
    const fermer = vi.fn()
    render(<Drawer inline type="Ticket" reference="TCK-000318" status="info" statusLabel="en cours" onClose={fermer} />)
    const tiroir = screen.getByRole('dialog', { name: 'Ticket TCK-000318' })
    expect(tiroir.getAttribute('aria-modal')).toBe('true')
    fireEvent.keyDown(tiroir, { key: 'Escape' })
    expect(fermer).toHaveBeenCalled()
  })
})
