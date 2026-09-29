// Extensions de contrat du lot 1 : rétrocompatibles, sans effet quand la propriété est absente.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { ConfirmDialog, CorrelationIdChip, DagView, FourElementsChecklist, FileDropzone, Hash, ProgressRing, QuitusCircuit, ResourceDetail, ResourceList, SideNav, SignDialog, StateMachineBar, TopBar, type ResourceDescriptor } from '../src'

afterEach(cleanup)
const H = '3f9a1c7e2b4d8f01a6c5e9b2d7f4a1c83e6b9d2f5a8c1e4b7d0f3a6c9e2b5d8f'

describe('QuitusCircuit', () => {
  it('trois quitus par défaut', () => {
    const { container } = render(<QuitusCircuit />)
    expect(container.querySelectorAll('li')).toHaveLength(3)
    expect(container.textContent).toContain('Quitus sécurité et conformité')
  })
  it('quatre quitus : sécurité et conformité en parallèle au même rang', () => {
    const { container } = render(<QuitusCircuit natures="quatre" quitus={{ tech: { state: 'signed' }, sec: { state: 'pending' }, conf: { state: 'signed', signer: 'Responsable conformité B' } }} />)
    const li = container.querySelectorAll('li')
    expect(li).toHaveLength(4)
    expect([...li].map((l) => l.querySelector('.gm-banner-title')?.textContent)).toEqual(['Quitus technique', 'Quitus sécurité', 'Quitus conformité', 'Quitus de la direction générale'])
    expect(li[1].querySelector('.gm-step-num')?.textContent).toBe('2')
    expect(li[2].textContent).toContain('en parallèle')
    expect(li[3].querySelector('.gm-step-num')?.textContent).toBe('3')
  })
})

describe('StateMachineBar', () => {
  const states = ['OUVERTE', 'EN_HOMOLOGATION', 'GELEE', 'CERTIFIEE'].map((k) => ({ key: k, label: k }))
  it('sans traversed : les états précédant le courant sont parcourus', () => {
    const { container } = render(<StateMachineBar states={states} current="GELEE" />)
    expect([...container.querySelectorAll('li')].map((l) => l.className)).toEqual(['gm-state-done', 'gm-state-done', 'gm-state-current', ''])
  })
  it('avec traversed : seuls les états fournis sont parcourus', () => {
    const { container } = render(<StateMachineBar states={states} current="GELEE" traversed={['OUVERTE']} allowed={['EN_HOMOLOGATION']} />)
    expect([...container.querySelectorAll('li')].map((l) => l.className)).toEqual(['gm-state-done', 'gm-state-next', 'gm-state-current', ''])
  })
})

describe('Hash', () => {
  it('sans onVerify : pas de bouton de vérification', () => {
    render(<Hash value={H} />)
    expect(screen.queryByRole('button', { name: "Vérifier l'empreinte" })).toBeNull()
  })
  it('vérification et verdict', () => {
    const verifier = vi.fn()
    const { rerender, container } = render(<Hash value={H} onVerify={verifier} />)
    fireEvent.click(screen.getByRole('button', { name: "Vérifier l'empreinte" }))
    expect(verifier).toHaveBeenCalledOnce()
    rerender(<Hash value={H} onVerify={verifier} verified />)
    expect(container.textContent).toContain('vérifiée')
    rerender(<Hash value={H} onVerify={verifier} verified={false} />)
    expect(container.querySelector('.gm-badge-crit')?.textContent).toContain('divergente')
  })
})

describe('CorrelationIdChip', () => {
  it('porte data-testid="correlation-id" sur l\'identifiant', () => {
    render(<CorrelationIdChip id="RQ-2026-0928-14" />)
    expect(screen.getByTestId('correlation-id').textContent).toBe('RQ-2026-0928-14')
  })
})

describe('ProgressRing', () => {
  it('valeur null : n/c et libellé accessible « non calculable »', () => {
    render(<ProgressRing label="Certification" value={null} />)
    expect(screen.getByRole('img', { name: 'Certification : non calculable' }).textContent).toBe('n/c')
  })
})

describe('ConfirmDialog', () => {
  it('onConfirm reçoit le motif saisi', () => {
    const confirmer = vi.fn()
    render(<ConfirmDialog inline title="Dégeler" confirmLabel="Dégeler la liaison" reason onConfirm={confirmer} onClose={() => {}} extra={<p>Contenu complémentaire</p>} />)
    expect(screen.getByText('Contenu complémentaire')).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/Motif/), { target: { value: '  Levée du gel après régularisation  ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Dégeler la liaison' }))
    expect(confirmer).toHaveBeenCalledWith('Levée du gel après régularisation')
  })
  it('sans champ de motif, onConfirm reçoit une chaîne vide', () => {
    const confirmer = vi.fn()
    render(<ConfirmDialog inline title="Publier" confirmLabel="Publier la version" onConfirm={confirmer} onClose={() => {}} />)
    fireEvent.click(screen.getByRole('button', { name: 'Publier la version' }))
    expect(confirmer).toHaveBeenCalledWith('')
  })
})

describe('SignDialog', () => {
  it('motif obligatoire : bouton de signature désactivé puis onSign(motif)', () => {
    const signer = vi.fn()
    render(<SignDialog inline title="Signer le quitus" action="Signer le quitus" hash={H} reason onSign={signer} extra={<p>Socle d'évaluation</p>} />)
    expect(screen.getByText("Socle d'évaluation")).toBeTruthy()
    const bouton = screen.getByRole('button', { name: 'Utiliser ma clé de sécurité' }) as HTMLButtonElement
    expect(bouton.disabled).toBe(true)
    fireEvent.change(screen.getByLabelText(/Motif/), { target: { value: 'Conditions de mise en service réunies' } })
    expect(bouton.disabled).toBe(false)
    fireEvent.click(bouton)
    expect(signer).toHaveBeenCalledWith('Conditions de mise en service réunies')
  })
  it('sans motif : onSign("") ; erreur affichée', () => {
    const signer = vi.fn()
    render(<SignDialog inline title="Signer" hash={H} onSign={signer} error="La clé de sécurité a été refusée." />)
    fireEvent.click(screen.getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(signer).toHaveBeenCalledWith('')
    expect(screen.getByText('La clé de sécurité a été refusée.')).toBeTruthy()
  })
  it('état error : « Réessayer » relance onSign', () => {
    const signer = vi.fn()
    render(<SignDialog inline title="Signer" hash={H} state="error" onSign={signer} />)
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }))
    expect(signer).toHaveBeenCalledOnce()
  })
})

describe('TopBar', () => {
  it('onMenu est branché sur « Replier la navigation »', () => {
    const menu = vi.fn()
    render(<TopBar onMenu={menu} />)
    fireEvent.click(screen.getByRole('button', { name: 'Replier la navigation' }))
    expect(menu).toHaveBeenCalledOnce()
  })
})

describe('SideNav', () => {
  it('onNavigate reçoit l\'adresse et empêche la navigation native', () => {
    const naviguer = vi.fn()
    render(<SideNav groups={[{ items: [{ key: 'l', label: 'Liaisons', icon: 'route', href: '/des/liaisons' }] }]} onNavigate={naviguer} />)
    const lien = screen.getByRole('link', { name: 'Liaisons' })
    const ev = new MouseEvent('click', { bubbles: true, cancelable: true })
    lien.dispatchEvent(ev)
    expect(naviguer).toHaveBeenCalledWith('/des/liaisons')
    expect(ev.defaultPrevented).toBe(true)
  })
})

describe('FileDropzone', () => {
  it('sans onFile : aucun champ fichier', () => {
    const { container } = render(<FileDropzone label="Preuve" />)
    expect(container.querySelector('input[type=file]')).toBeNull()
  })
  it('dépôt par glisser et par le champ masqué', () => {
    const recu = vi.fn()
    const { container } = render(<FileDropzone label="Preuve" accept="PDF, PNG" onFile={recu} />)
    const f = new File(['x'], 'preuve.pdf', { type: 'application/pdf' })
    fireEvent.drop(container.querySelector('.gm-dropzone')!, { dataTransfer: { files: [f] } })
    expect(recu).toHaveBeenCalledWith(f)
    const champ = container.querySelector('input[type=file]') as HTMLInputElement
    expect(champ.accept).toBe('.pdf,.png')
    fireEvent.change(champ, { target: { files: [f] } })
    expect(recu).toHaveBeenCalledTimes(2)
  })
})

type Ligne = { id: string; nom: string; etat: string; transitionsPermises: string[] }
const desc: ResourceDescriptor<Ligne> = {
  key: 'liaison', label: { singular: 'liaison', plural: 'liaisons', gender: 'f' }, idField: 'id', titleField: 'nom', statusField: 'etat',
  endpoints: { list: '/l', get: '/l/{id}' },
  fields: [{ name: 'nom', label: 'Institution', kind: 'text', inList: true, inDetail: true, sortable: true }],
  actions: [{ key: 'ouvrir', label: 'Ouvrir une liaison', scope: 'page', permission: 'P', variant: 'primary', confirm: 'none' }],
  statusDictionary: { OUVERTE: { label: 'ouverte', status: 'neutral' }, GELEE: { label: 'gelée', status: 'crit' }, CERTIFIEE: { label: 'certifiée', status: 'ok' } },
  permissions: { list: 'P', read: 'P' },
}
const lignes: Ligne[] = [{ id: 'LIA-1', nom: 'Établissement de paiement A', etat: 'GELEE', transitionsPermises: [] }]

describe('ResourceDetail', () => {
  it('onglet contrôlé, audit masqué, traversed transmis', () => {
    const onglet = vi.fn()
    const { rerender, container } = render(<ResourceDetail descriptor={desc} row={lignes[0]} audit={false} traversed={['OUVERTE']} onTabChange={onglet} timeline={[]} />)
    expect(screen.queryByRole('tab', { name: 'Audit' })).toBeNull()
    expect([...container.querySelectorAll('.gm-states li')].map((l) => l.className)).toEqual(['gm-state-done', 'gm-state-current', ''])
    fireEvent.click(screen.getByRole('tab', { name: 'Détail' }))
    expect(onglet).toHaveBeenCalledWith('detail')
    const avecOnglet = { ...desc, tabs: [{ key: 'preuves', label: 'Preuves', render: () => <p>Contenu des preuves</p> }] }
    rerender(<ResourceDetail descriptor={avecOnglet} row={lignes[0]} audit={false} tab="preuves" />)
    expect(screen.getByText('Contenu des preuves')).toBeTruthy()
  })
})

describe('ResourceList', () => {
  it('actions de page en mode embarqué seulement sur demande', () => {
    const { rerender } = render(<ResourceList descriptor={desc} rows={lignes} embedded />)
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison' })).toBeNull()
    rerender(<ResourceList descriptor={desc} rows={lignes} embedded showPageActions />)
    expect(screen.getByRole('button', { name: 'Ouvrir une liaison' })).toBeTruthy()
    expect(screen.queryByRole('heading', { level: 1 })).toBeNull()
  })
  it('onRowOpen remplace le tiroir ; onQuery suit tri, recherche et pagination', () => {
    const ouvrir = vi.fn()
    const requete = vi.fn()
    render(<ResourceList descriptor={desc} rows={lignes} total={120} onRowOpen={ouvrir} onQuery={requete} />)
    fireEvent.click(screen.getByText('Établissement de paiement A'))
    expect(ouvrir).toHaveBeenCalledWith(lignes[0])
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: /Institution/ }))
    expect(requete).toHaveBeenLastCalledWith(expect.objectContaining({ sort: { key: 'nom', dir: 'asc' } }))
    fireEvent.change(screen.getByRole('searchbox', { name: 'Rechercher' }), { target: { value: 'Banque' } })
    expect(requete).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'Banque', page: 1 }))
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    expect(requete).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }))
  })
  it('drawerRender fournit le contenu des onglets du tiroir', () => {
    render(<ResourceList descriptor={desc} rows={lignes} drawerRender={(tab, r) => <p>{tab + ' de ' + r.id}</p>} />)
    fireEvent.click(screen.getByText('Établissement de paiement A'))
    expect(screen.getByText('detail de LIA-1')).toBeTruthy()
  })
})

describe('DagView', () => {
  const noeuds = [{ code: 'OP-1', label: 'Recette', owner: 'GIMAC', phase: 'p1', state: 'eligible' as const, elements: {} }]
  it('onSelect au clic et à Entrée ; selected marque le nœud', () => {
    const choisir = vi.fn()
    const { container } = render(<DagView phases={[{ key: 'p1', label: 'Raccordement' }]} nodes={noeuds} onSelect={choisir} selected="OP-1" />)
    const n = container.querySelector('.gm-dag-node') as HTMLElement
    expect(n.className).toContain('gm-dag-node-sel')
    expect(n.getAttribute('aria-current')).toBe('true')
    fireEvent.click(n)
    fireEvent.keyDown(n, { key: 'Enter' })
    expect(choisir).toHaveBeenCalledTimes(2)
    expect(choisir).toHaveBeenCalledWith('OP-1')
  })
})

describe('ProgressRing compact', () => {
  it('anneau de 24 px, pourcentage à côté, sans légende', () => {
    const { container } = render(<ProgressRing label="Certification" value={62} compact status="ok" statusLabel="certifiée" />)
    expect(container.querySelector('svg')?.getAttribute('width')).toBe('24')
    expect(container.textContent).toBe('62 %')
    expect(container.querySelector('.gm-ring-label')).toBeNull()
  })
})

describe('FourElementsChecklist', () => {
  it('« pending » rendu en attente ; onAction branché sur le bouton', () => {
    const agir = vi.fn()
    const { container } = render(
      <FourElementsChecklist
        elements={{ owner: true, proof: 'rapport.pdf', timestamp: 'pending', validation: false }}
        actions={{ validation: 'Demander la validation' }}
        onAction={agir}
      />,
    )
    expect(container.querySelectorAll('.gm-check4-missing')).toHaveLength(2)
    expect(container.querySelector('.gm-badge-warn')?.textContent).toContain('en attente')
    fireEvent.click(screen.getByRole('button', { name: 'Demander la validation' }))
    expect(agir).toHaveBeenCalledWith('validation')
  })
})
