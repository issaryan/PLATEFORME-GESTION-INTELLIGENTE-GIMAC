// Composants universels du CRUD (§8.3) : formulaire de tous les types de champ, étapes et erreurs ;
// liste (actions de page, en masse, de ligne, confirmation, signature, tri, pagination, tiroir) ;
// fiche (onglets, chronologie, audit, tiroir) ; suppression ou archivage.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { ResourceDelete, ResourceDetail, ResourceForm, ResourceList, type ResourceDescriptor } from '../src'

afterEach(cleanup)

type Ligne = {
  id: string; nom: string; etat: string; montant: number; date: string; type: string; actif: boolean; tags: string[]; note: string
  debut: string; periode: string; mode: string; bascule: boolean; piece: string; code: string; qte: number; ref: string
}
const ligne: Ligne = {
  id: 'L-1', nom: 'Banque B', etat: 'OUVERTE', montant: 125000, date: '2026-09-29', type: 'A', actif: true, tags: ['RETRAIT', 'PAIEMENT'], note: '',
  debut: '', periode: '', mode: 'D', bascule: false, piece: '', code: 'X-1', qte: 3, ref: 'R',
}
const run = vi.fn()
const descripteur = (o: Partial<ResourceDescriptor<Ligne>> = {}): ResourceDescriptor<Ligne> => ({
  key: 'ligne', label: { singular: 'liaison', plural: 'liaisons', gender: 'f' }, idField: 'id', titleField: 'nom', statusField: 'etat',
  endpoints: { list: '/l', get: '/l/{id}' }, permissions: { list: 'L', read: 'L' },
  statusDictionary: { OUVERTE: { label: 'ouverte', status: 'info' }, CLOSE: { label: 'close', status: 'neutral' } },
  fields: [
    { name: 'nom', label: 'Nom', kind: 'text', inList: true, inDetail: true, inForm: true, sortable: true, required: true },
    { name: 'etat', label: 'État', kind: 'status', inList: true, inDetail: true, filterable: true },
    { name: 'montant', label: 'Montant', kind: 'money', inList: true, inDetail: true, inForm: true },
    { name: 'qte', label: 'Quantité', kind: 'number', inList: true, inForm: true, unit: 'j' },
    { name: 'date', label: 'Date', kind: 'date', inList: true, inDetail: true, inForm: true, filterable: true },
    { name: 'debut', label: 'Début', kind: 'datetime', inDetail: true, inForm: true },
    { name: 'periode', label: 'Période', kind: 'daterange', inForm: 'create' },
    { name: 'type', label: 'Type', kind: 'select', inList: true, inDetail: true, inForm: true, options: [{ value: 'A', label: 'Type A' }], filterable: true },
    { name: 'ref', label: 'Référence', kind: 'searchSelect', inDetail: true, inForm: true, options: [{ value: 'R', label: 'Réf. R' }] },
    { name: 'tags', label: 'Services', kind: 'multiSelect', inDetail: true, inForm: true, options: [{ value: 'RETRAIT', label: 'Retrait' }] },
    { name: 'actif', label: 'Actif', kind: 'checkbox', inDetail: true, inForm: true },
    { name: 'mode', label: 'Mode', kind: 'radio', inDetail: true, inForm: true, options: [{ value: 'D', label: 'Direct' }, { value: 'I', label: 'Indirect' }] },
    { name: 'bascule', label: 'Bascule', kind: 'switch', inDetail: true, inForm: 'edit' },
    { name: 'piece', label: 'Pièce', kind: 'file', inForm: 'create', accept: 'PDF', maxSize: '20 Mo' },
    { name: 'code', label: 'Code', kind: 'mono', inDetail: true, inForm: true, permission: 'SECRET' },
    { name: 'note', label: 'Note', kind: 'textarea', inDetail: true, inForm: true, width: 2 },
  ],
  actions: [
    { key: 'creer', label: 'Créer', scope: 'page', permission: 'L', variant: 'primary', run },
    { key: 'exporter', label: 'Exporter', scope: 'page', permission: 'L', run },
    { key: 'lot', label: 'Traiter la sélection', scope: 'bulk', permission: 'L', run },
    { key: 'clore', label: 'Clore', scope: 'row', permission: 'L', confirm: 'confirm', reason: true, consequences: ['Définitif'], run },
    { key: 'signer', label: 'Signer', scope: 'row', permission: 'L', confirm: 'sign', run },
    { key: 'voir', label: 'Voir', scope: 'row', permission: 'L', variant: 'primary', run },
  ],
  ...o,
})

describe('ResourceForm', () => {
  it('création : tous les types de champ, saisie et soumission', () => {
    const soumettre = vi.fn()
    const { container } = render(<ResourceForm descriptor={descripteur()} onSubmit={soumettre} />)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toContain('liaison')
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: 'Banque Z' } })
    fireEvent.change(container.querySelector('textarea[name="note"]')!, { target: { value: 'Note' } })
    fireEvent.change(container.querySelector('select[name="type"]')!, { target: { value: 'A' } })
    fireEvent.click(screen.getByLabelText('Actif'))
    fireEvent.click(screen.getByLabelText('Indirect'))
    fireEvent.change(screen.getByRole('combobox', { name: /Référence/ }), { target: { value: 'R' } })
    expect(screen.getByRole('listbox')).toBeTruthy()
    fireEvent.submit(container.querySelector('form')!)
    expect(soumettre.mock.calls[0][0]).toMatchObject({ nom: 'Banque Z', note: 'Note', type: 'A', actif: true, mode: 'I', ref_q: 'R' })
  })
  it('modification : champs d’édition, erreurs du service et synthèse des champs à corriger', () => {
    const annuler = vi.fn()
    render(<ResourceForm descriptor={descripteur({ label: { singular: 'institution', plural: 'institutions', gender: 'f' } })} mode="edit" initial={{ nom: 'Banque B', bascule: true }}
      errors={{ nom: 'Obligatoire', inconnu: 'Autre' }} serverError={{ code: 'X', message: 'Refus du service', correlationId: 'c-1' }} onCancel={annuler} />)
    expect(screen.getByText('Refus du service')).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Nom' })).toBeTruthy()
    expect(screen.getByText(/2 champs à corriger/)).toBeTruthy()
    expect(screen.getByRole('switch', { name: 'Bascule' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    expect(annuler).toHaveBeenCalled()
  })
  it('genre masculin, élision, étapes et signature', () => {
    const d = descripteur({ label: { singular: 'ouvrage', plural: 'ouvrages', gender: 'm' } })
    const { rerender } = render(<ResourceForm descriptor={d} steps={[{ label: 'Identité', fields: ['nom'] }, { label: 'Montant', fields: ['montant'] }]} confirm="sign" />)
    expect(screen.getByRole('button', { name: 'Étape suivante' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Étape suivante' }))
    expect(screen.getByRole('button', { name: 'Étape précédente' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Étape précédente' }))
    rerender(<ResourceForm descriptor={descripteur({ label: { singular: 'dossier', plural: 'dossiers', gender: 'm' } })} mode="edit" embedded />)
    expect(screen.queryByRole('heading', { level: 1 })).toBeNull()
  })
})

describe('ResourceList', () => {
  it('actions de page, en masse et de ligne ; confirmation motivée ; signature', () => {
    run.mockClear()
    render(<ResourceList descriptor={descripteur()} rows={[ligne, { ...ligne, id: 'L-2', nom: 'Banque D', etat: 'INCONNU' }]} total={2} permissions={['L']} />)
    expect(screen.getByRole('heading', { level: 1, name: 'Liaisons' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }))
    fireEvent.click(screen.getByRole('button', { name: 'Exporter' }))
    expect(run).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('button', { name: 'Traiter la sélection' })).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Clore' })[0])
    const d = screen.getByRole('alertdialog')
    fireEvent.change(within(d).getByRole('textbox'), { target: { value: 'Motif suffisamment détaillé' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Clore' }))
    expect(run).toHaveBeenCalledTimes(3)
    fireEvent.click(screen.getAllByRole('button', { name: 'Plus d\'actions' })[0])
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Signer' }))
    expect(screen.getByRole('dialog').textContent).toContain('Signer')
    fireEvent.click(within(screen.getByRole('dialog')).getAllByRole('button', { name: /Fermer|Annuler/ })[0])
  })
  it('tri, pagination, taille de page, recherche : requête notifiée ; tiroir et chronologie', () => {
    const q = vi.fn()
    render(<ResourceList descriptor={descripteur()} rows={[ligne]} total={60} onQuery={q} embedded showPageActions liveUpdates timeline={[{ event: 'Ouverte', at: '2026-09-29' }]} density="compact" activeFilters={[{ key: 'etat', label: 'ouverte' }]} />)
    fireEvent.click(screen.getByRole('button', { name: 'Nom' }))
    fireEvent.click(screen.getByRole('button', { name: 'Nom' }))
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    fireEvent.change(screen.getByRole('combobox', { name: 'Taille de page' }), { target: { value: '50' } })
    fireEvent.change(screen.getByRole('searchbox', { name: 'Rechercher' }), { target: { value: 'banque' } })
    expect(q.mock.calls.map((c) => c[0])).toEqual(expect.arrayContaining([expect.objectContaining({ sort: { key: 'nom', dir: 'desc' } }), expect.objectContaining({ page: 2 }), expect.objectContaining({ pageSize: 50 }), expect.objectContaining({ q: 'banque' })]))
    fireEvent.click(screen.getByText('Banque B'))
    const tiroir = screen.getByRole('dialog')
    fireEvent.click(within(tiroir).getByRole('tab', { name: 'Chronologie' }))
    expect(within(tiroir).getByText('Ouverte')).toBeTruthy()
    fireEvent.click(within(tiroir).getByRole('tab', { name: 'Audit' }))
    expect(within(tiroir).getByText(/Aucun élément/)).toBeTruthy()
  })
  it('rendu du tiroir délégué, lignes ouvertes par l’appelant, états', () => {
    const rendu = vi.fn(() => <p>contenu délégué</p>)
    const { rerender } = render(<ResourceList descriptor={descripteur()} rows={[ligne]} drawerRender={rendu} inline />)
    fireEvent.click(screen.getByText('Banque B'))
    expect(screen.getByText('contenu délégué')).toBeTruthy()
    rerender(<ResourceList descriptor={descripteur({ label: { singular: 'dossier', plural: 'dossiers', gender: 'm' } })} rows={[]} state="empty" />)
    expect(document.querySelector('.gm-empty')).toBeTruthy()
    rerender(<ResourceList descriptor={descripteur()} rows={[]} state="error" error={{ code: 'X', message: 'Indisponible', correlationId: 'c-503' }} />)
    expect(screen.getByText('c-503')).toBeTruthy()
  })
})

describe('ResourceDetail', () => {
  const d = descripteur({ tabs: [{ key: 'operations', label: 'Opérations', render: (r) => <p>Opérations de {r.nom}</p> }, { key: 'vide', label: 'Vide' }], timeline: () => '/h' })
  it('onglets du descripteur, chronologie, audit, onglet sans rendu ; champs selon permission', () => {
    const changer = vi.fn()
    render(<ResourceDetail descriptor={d} row={ligne} timeline={[{ event: 'Ouverte', at: '2026-09-29' }]} audit={['c-1']} onTabChange={changer} permissions={['L']} traversed={['OUVERTE']} />)
    expect(screen.getByText('oui')).toBeTruthy()
    expect(screen.getByText('non')).toBeTruthy()
    expect(screen.getByText('Type A')).toBeTruthy()
    expect(screen.queryByText('X-1')).toBeNull()
    fireEvent.click(screen.getByRole('tab', { name: 'Opérations' }))
    expect(screen.getByText('Opérations de Banque B')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Vide' }))
    expect(document.querySelector('.gm-empty')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Chronologie' }))
    expect(screen.getByText('Ouverte')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Audit' }))
    expect(screen.getByText('c-1')).toBeTruthy()
    expect(changer).toHaveBeenCalledWith('audit')
  })
  it('actions de ligne en tête (plus de trois : menu), mode tiroir, ligne absente', () => {
    const d2 = descripteur({ actions: [...descripteur().actions, { key: 'autre', label: 'Autre', scope: 'row', permission: 'L', run }, { key: 'encore', label: 'Encore', scope: 'row', permission: 'L', run }] })
    const { rerender } = render(<ResourceDetail descriptor={d2} row={{ ...ligne, note: 'Texte' }} onAction={run} />)
    expect(screen.getByRole('button', { name: 'Plus d\'actions' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Voir' }))
    rerender(<ResourceDetail descriptor={d} row={ligne} mode="drawer" />)
    expect(screen.queryByRole('tablist')).toBeNull()
    rerender(<ResourceDetail descriptor={descripteur({ statusField: undefined, statusDictionary: undefined })} row={null as unknown as Ligne} />)
    expect(screen.getByRole('heading', { level: 1 })).toBeTruthy()
  })
})

describe('ResourceDelete', () => {
  it('archivage par défaut, suppression irréversible, signature', () => {
    const fin = vi.fn()
    const { rerender } = render(<ResourceDelete descriptor={descripteur()} row={ligne} onDone={fin} />)
    expect(screen.getByRole('alertdialog').textContent).toMatch(/Archiver/)
    rerender(<ResourceDelete descriptor={descripteur({ endpoints: { list: '/l', get: '/l/1', delete: '/l/1' }, label: { singular: 'dossier', plural: 'dossiers', gender: 'm' } })} row={ligne} onDone={fin} />)
    expect(screen.getByRole('alertdialog').textContent).toMatch(/Supprimer/)
    rerender(<ResourceDelete descriptor={descripteur({ label: { singular: 'dossier', plural: 'dossiers', gender: 'm' } })} row={ligne} confirm="sign" signHash={'a'.repeat(64)} inline />)
    expect(screen.getByRole('dialog')).toBeTruthy()
  })
})
