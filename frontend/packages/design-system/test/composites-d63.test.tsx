// Composites de la tranche DDRM et du cadrage (D-63) : assemblages de composants et de classes existants,
// sans nouvelle feuille de style ni valeur en dur, promus depuis les écrans de l'application du département.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { ActionBar, Button, InlineAlert, LinkList, PageHeader, PageSection, ResourceDetail, ResourceForm, ResourceList, StatGrid, Tag, Toast, ToastRegion, type ResourceDescriptor } from '../src'

afterEach(cleanup)

describe('PageHeader', () => {
  it('titre, surtitre, ligne de contexte et actions', () => {
    render(<PageHeader title="Tableau de bord" overline="Registre" meta={<Tag>Chef de la DDRM</Tag>} actions={<Button>Ouvrir une liaison</Button>} />)
    expect(screen.getByRole('heading', { level: 1, name: 'Tableau de bord' })).toBeTruthy()
    expect(screen.getByText('Registre').className).toContain('gm-type-micro')
    expect(screen.getByText('Chef de la DDRM')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Ouvrir une liaison' })).toBeTruthy()
  })
  it('titre seul : ni surtitre, ni ligne de contexte, ni actions', () => {
    const { container } = render(<PageHeader title="Liaisons" className="x" />)
    expect(container.querySelector('header')?.className).toBe('gm-resource-head x')
    expect(container.querySelectorAll('.gm-row')).toHaveLength(0)
    expect(container.querySelector('.gm-type-micro')).toBeNull()
  })
})

describe('PageSection', () => {
  it('région nommée par son titre, actions', () => {
    render(<PageSection title="Mes projets" actions={<a href="/x">Voir tout</a>}><p>contenu</p></PageSection>)
    const r = screen.getByRole('region', { name: 'Mes projets' })
    expect(within(r).getByRole('heading', { level: 2 })).toBeTruthy()
    expect(within(r).getByRole('link', { name: 'Voir tout' })).toBeTruthy()
    expect(within(r).getByText('contenu')).toBeTruthy()
  })
  it('niveau 3, sans actions', () => {
    render(<PageSection title="Détail" level={3} className="y" />)
    expect(screen.getByRole('heading', { level: 3, name: 'Détail' })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Détail' }).className).toBe('gm-col y')
  })
})

describe('StatGrid', () => {
  it('une StatCard par indicateur, groupe nommé, carte cliquable', () => {
    const ouvrir = vi.fn()
    render(<StatGrid label="Registre" items={[{ key: 'a', label: 'Candidats', value: 3 }, { key: 'b', label: 'Liaisons en cadrage', value: 2, status: 'info', statusLabel: 'à suivre', onClick: ouvrir }]} />)
    const g = screen.getByRole('group', { name: 'Registre' })
    expect(g.querySelectorAll('.gm-stat')).toHaveLength(2)
    fireEvent.click(within(g).getByRole('button'))
    expect(ouvrir).toHaveBeenCalledOnce()
    expect(within(g).getByText('à suivre')).toBeTruthy()
  })
})

describe('LinkList', () => {
  it('liens, complément et statut', () => {
    render(<LinkList label="Mes dossiers" items={[
      { key: '1', href: '/des/institutions/1', label: 'Fintech Kribi Pay', status: 'info', statusLabel: 'Candidat' },
      { key: '2', href: '/des/liaisons/2', label: 'Banque B', meta: 'LIA-2025-102' },
      { key: '3', href: '/des/liaisons/3', label: 'Banque D', status: 'warn' },
      { key: '4', href: '/des/liaisons/4', label: 'Sans complément' },
    ]} />)
    const l = screen.getByRole('list', { name: 'Mes dossiers' })
    expect(within(l).getAllByRole('listitem')).toHaveLength(4)
    expect(within(l).getByRole('link', { name: 'Fintech Kribi Pay' }).getAttribute('href')).toBe('/des/institutions/1')
    expect(within(l).getByText('Candidat')).toBeTruthy()
    expect(within(l).getByText('LIA-2025-102')).toBeTruthy()
    expect(within(l).getByText('warn')).toBeTruthy()
  })
  it('liste vide : texte explicite, ou rien', () => {
    const { container, rerender } = render(<LinkList label="Mes projets" items={[]} empty="Aucun projet." />)
    expect(screen.getByText('Aucun projet.')).toBeTruthy()
    rerender(<LinkList label="Mes projets" items={[]} />)
    expect(container.textContent).toBe('')
  })
})

describe('ActionBar', () => {
  it('alignement à droite par défaut, groupe nommé', () => {
    render(<ActionBar label="Actions du formulaire"><Button>Annuler</Button></ActionBar>)
    const g = screen.getByRole('group', { name: 'Actions du formulaire' })
    expect(g.style.justifyContent).toBe('flex-end')
  })
  it('écartés ou à gauche, sans rôle quand il n’est pas nommé', () => {
    const { container, rerender } = render(<ActionBar align="between" className="z"><Button>A</Button></ActionBar>)
    const d = container.firstElementChild as HTMLElement
    expect(d.style.justifyContent).toBe('space-between')
    expect(d.getAttribute('role')).toBeNull()
    expect(d.className).toBe('gm-row z')
    rerender(<ActionBar align="start"><Button>A</Button></ActionBar>)
    expect((container.firstElementChild as HTMLElement).style.justifyContent).toBe('flex-start')
  })
})

describe('ResourceForm : introduction', () => {
  type Ligne = { id: string; typeProjet: string }
  const descripteur: ResourceDescriptor<Ligne> = {
    key: 'liaison', label: { singular: 'liaison', plural: 'liaisons', gender: 'f' }, idField: 'id', titleField: 'id',
    endpoints: { list: '', get: '' }, permissions: { list: 'X', read: 'X' }, actions: [],
    fields: [{ name: 'typeProjet', label: 'Type de projet', kind: 'radio', inForm: true, options: [{ value: 'A', label: 'Carte' }] }],
  }
  it('texte rendu sous le titre, avant les champs', () => {
    const { container } = render(<ResourceForm descriptor={descripteur} title="Ouvrir une liaison" intro={<InlineAlert level="info">L'institution passera en homologation.</InlineAlert>} />)
    const titre = screen.getByRole('heading', { level: 1 })
    expect(titre.nextElementSibling?.textContent).toContain("L'institution passera en homologation.")
    expect(container.querySelector('.gm-form-grid')).toBeTruthy()
  })
  it('sans introduction : rendu inchangé', () => {
    render(<ResourceForm descriptor={descripteur} title="Ouvrir une liaison" />)
    expect(screen.getByRole('heading', { level: 1 }).nextElementSibling?.className).toBe('gm-form-grid')
  })
})

describe('ResourceDetail : résumé sous l’en-tête', () => {
  type Liaison = { id: string; nom: string; etat: string }
  const d: ResourceDescriptor<Liaison> = {
    key: 'liaison', label: { singular: 'liaison', plural: 'liaisons', gender: 'f' }, idField: 'id', titleField: 'nom', statusField: 'etat',
    endpoints: { list: '', get: '' }, permissions: { list: 'X', read: 'X' }, actions: [], fields: [{ name: 'nom', label: 'Nom', kind: 'text', inDetail: true }],
    statusDictionary: { OUVERTE: { label: 'ouverte', status: 'neutral' } },
  }
  it('résumé entre l’en-tête et la barre d’états', () => {
    const { container } = render(<ResourceDetail descriptor={d} row={{ id: 'LIA-1', nom: 'Banque B', etat: 'OUVERTE' }} audit={false} summary={<p data-testid="resume">Cadrage</p>} />)
    expect(container.querySelector('header.gm-objhead')?.nextElementSibling).toBe(screen.getByTestId('resume'))
  })
  it('sans résumé : rendu inchangé', () => {
    const { container } = render(<ResourceDetail descriptor={d} row={{ id: 'LIA-1', nom: 'Banque B', etat: 'OUVERTE' }} audit={false} />)
    expect(container.querySelector('header.gm-objhead')?.nextElementSibling?.textContent).toContain('ouverte')
  })
})

describe('ResourceList : « Plus d’actions » ouvre le tiroir de la ligne', () => {
  type Piece = { code: string; libelle: string }
  const run = vi.fn()
  const d: ResourceDescriptor<Piece> = {
    key: 'piece', label: { singular: 'pièce', plural: 'pièces', gender: 'f' }, idField: 'code', titleField: 'libelle',
    endpoints: { list: '', get: '' }, permissions: { list: 'X', read: 'X' }, fields: [{ name: 'libelle', label: 'Pièce', kind: 'text', inList: true, inDetail: true }],
    actions: [
      { key: 'valider', label: 'Valider la pièce', scope: 'row', permission: 'X', run },
      { key: 'rejeter', label: 'Rejeter la pièce', scope: 'row', permission: 'X', variant: 'danger', run },
    ],
  }
  it('la première action est dans la ligne, les autres dans le tiroir', () => {
    render(<ResourceList descriptor={d} rows={[{ code: 'STATUTS', libelle: 'Statuts' }]} embedded />)
    expect(screen.queryByRole('button', { name: 'Rejeter la pièce' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Plus d\'actions' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Rejeter la pièce' }))
    expect(run).toHaveBeenCalledOnce()
    expect(screen.queryByRole('dialog')).toBeNull()
  })
  it('un clic sur l’action de la ligne agit sans ouvrir le tiroir ; un clic sur la ligne l’ouvre', () => {
    render(<ResourceList descriptor={d} rows={[{ code: 'STATUTS', libelle: 'Statuts' }]} embedded />)
    fireEvent.click(screen.getByRole('button', { name: 'Valider la pièce' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(screen.getByText('Statuts'))
    expect(screen.getByRole('dialog')).toBeTruthy()
  })
  it('avec onRowOpen, le bouton ouvre la ligne chez l’appelant', () => {
    const ouvrir = vi.fn()
    render(<ResourceList descriptor={d} rows={[{ code: 'STATUTS', libelle: 'Statuts' }]} embedded onRowOpen={ouvrir} />)
    fireEvent.click(screen.getByRole('button', { name: 'Plus d\'actions' }))
    expect(ouvrir).toHaveBeenCalledWith({ code: 'STATUTS', libelle: 'Statuts' })
  })
})

describe('ToastRegion', () => {
  it('place les retours en bas à droite, rien sans retour', () => {
    const { container, rerender } = render(<ToastRegion><Toast>Action effectuée</Toast></ToastRegion>)
    const r = container.firstElementChild as HTMLElement
    expect(r.style.position).toBe('fixed')
    expect(within(r).getByRole('status').textContent).toContain('Action effectuée')
    rerender(<ToastRegion>{null}</ToastRegion>)
    expect(container.textContent).toBe('')
  })
})
