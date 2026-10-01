// Modale CRUD standard (D-63) : CrudDialog, assemblage de Dialog (voile, focus piégé, Échap, aria-modal,
// titre et sous-titre de contexte, pied d'actions) et d'un formulaire natif, adaptée au contexte ;
// ResourceForm présenté en dialogue ; carte
// d'indicateur cliquable au clavier ; navigation latérale à la hauteur de la coquille.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { Button, CrudDialog, QrCode, ResourceForm, SideNav, StatCard, TextField, type ResourceDescriptor } from '../src'

afterEach(cleanup)

describe('CrudDialog : modale CRUD standard', () => {
  it('modification : dialogue modal titré avec sous-titre de contexte, formulaire, pied Annuler et bouton principal relié au formulaire', () => {
    const soumettre = vi.fn()
    const fermer = vi.fn()
    render(
      <CrudDialog title="Modifier la fiche" subject="c.ndong · Carine Ndong" description="Données descriptives du compte" onClose={fermer} onSubmit={soumettre} submitLabel="Enregistrer la fiche">
        <TextField name="nom" label="Nom" value="" onChange={() => undefined} />
      </CrudDialog>,
    )
    const d = screen.getByRole('dialog', { name: 'Modifier la fiche' })
    expect(d.getAttribute('aria-modal')).toBe('true')
    expect(d.className).toContain('gm-dialog-lg')
    expect(d.className).toContain('gm-crud-edit')
    expect(d.getAttribute('aria-describedby')!.split(' ')).toHaveLength(2)
    expect(within(d).getByText('c.ndong · Carine Ndong').className).toBe('gm-dialog-sujet')
    const principal = within(d).getByRole('button', { name: 'Enregistrer la fiche' }) as HTMLButtonElement
    const form = d.querySelector('form')!
    expect(principal.type).toBe('submit')
    expect(principal.getAttribute('form')).toBe(form.id)
    expect(form.getAttribute('aria-labelledby')).toBe(d.getAttribute('aria-labelledby'))
    fireEvent.submit(form)
    expect(soumettre).toHaveBeenCalledWith('')
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    expect(fermer).toHaveBeenCalledTimes(1)
  })

  it('Échap et clic sur le voile ferment ; focus placé dans le dialogue puis piégé', () => {
    const fermer = vi.fn()
    const { container } = render(
      <CrudDialog context="create" id="dlg" title="Créer un compte" onClose={fermer} onSubmit={() => undefined} submitLabel="Créer le compte">
        <TextField name="identifiant" label="Identifiant" value="" onChange={() => undefined} />
      </CrudDialog>,
    )
    const d = screen.getByRole('dialog', { name: 'Créer un compte' })
    expect(d.contains(document.activeElement)).toBe(true)
    const boutons = within(d).getAllByRole('button')
    boutons[boutons.length - 1].focus()
    fireEvent.keyDown(d, { key: 'Tab' })
    expect(d.contains(document.activeElement)).toBe(true)
    fireEvent.keyDown(d, { key: 'Tab', shiftKey: true })
    fireEvent.keyDown(d, { key: 'Escape' })
    fireEvent.mouseDown(container.querySelector('.gm-overlay')!)
    expect(fermer).toHaveBeenCalledTimes(2)
    expect(within(d).getByRole('button', { name: 'Créer le compte' }).innerHTML).toContain('svg')
  })

  it('envoi en cours : bouton occupé, ni Échap, ni voile, ni Annuler ne ferment ; soumission ignorée ; refus du service affiché', () => {
    const soumettre = vi.fn()
    const fermer = vi.fn()
    const { container } = render(
      <CrudDialog title="Appliquer" onClose={fermer} onSubmit={soumettre} submitLabel="Appliquer la politique" submitting danger size="sm"
        error={{ title: 'Refus du service', text: 'Plancher non respecté', correlationId: 'c-1' }} cancelLabel="Fermer" extraActions={<Button>Étape précédente</Button>}>
        <p>corps</p>
      </CrudDialog>,
    )
    const d = screen.getByRole('dialog')
    expect(d.className).toContain('gm-dialog-sm')
    expect(within(d).getByRole('alert').textContent).toContain('Plancher non respecté')
    expect((within(d).getByRole('button', { name: 'Fermer' }) as HTMLButtonElement).disabled).toBe(true)
    expect(within(d).getByRole('button', { name: /Appliquer la politique/ }).className).toContain('gm-btn-danger')
    expect(within(d).getByRole('button', { name: /Appliquer la politique/ }).getAttribute('aria-busy')).toBe('true')
    expect(within(d).getByRole('button', { name: 'Étape précédente' })).toBeTruthy()
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).not.toHaveBeenCalled()
    fireEvent.keyDown(d, { key: 'Escape' })
    fireEvent.mouseDown(container.querySelector('.gm-overlay')!)
    expect(fermer).not.toHaveBeenCalled()
  })

  it('formulaire modifié : la fermeture demande de confirmer l’abandon ; reprise ou abandon', () => {
    const fermer = vi.fn()
    render(<CrudDialog title="Modifier la fiche" onClose={fermer} onSubmit={() => undefined} submitLabel="Enregistrer" dirty><p>corps</p></CrudDialog>)
    const d = screen.getByRole('dialog')
    fireEvent.keyDown(d, { key: 'Escape' })
    expect(fermer).not.toHaveBeenCalled()
    expect(within(d).getByText('Abandonner la saisie ?')).toBeTruthy()
    fireEvent.submit(d.querySelector('form')!)
    fireEvent.keyDown(d, { key: 'Escape' })
    expect(within(d).queryByText('Abandonner la saisie ?')).toBeNull()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Reprendre la saisie' }))
    expect(within(d).getByRole('button', { name: 'Enregistrer' })).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Abandonner les modifications' }))
    expect(fermer).toHaveBeenCalledTimes(1)
  })

  it('suppression : mise en garde, case irréversible, bouton danger, rappel de l’objet et conséquences ; alertdialog', () => {
    const soumettre = vi.fn()
    render(<CrudDialog context="delete" title="Supprimer le compte" object={[{ key: 'Identifiant', value: 'c.ndong' }]} consequences={['Sessions fermées']} onClose={() => undefined} onSubmit={soumettre} submitLabel="Supprimer le compte" />)
    const d = screen.getByRole('alertdialog', { name: 'Supprimer le compte' })
    expect(d.className).toContain('gm-crud-delete')
    expect(within(d).getByText('Action définitive : elle ne pourra pas être annulée.')).toBeTruthy()
    expect(within(d).getByText('c.ndong')).toBeTruthy()
    expect(within(d).getByText('Sessions fermées')).toBeTruthy()
    const b = within(d).getByRole('button', { name: /Supprimer le compte/ }) as HTMLButtonElement
    expect(b.className).toContain('gm-btn-danger')
    expect(b.disabled).toBe(true)
    expect(b.getAttribute('title')).toBe('Cochez la case de confirmation')
    fireEvent.click(within(d).getByLabelText('Je confirme : cette action est irréversible.'))
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).toHaveBeenCalledWith('')
  })

  it('action sensible : motif obligatoire transmis, réauthentification annoncée ; confirmation simple', () => {
    const soumettre = vi.fn()
    const { rerender } = render(<CrudDialog context="sensitive" reauth title="Exiger des actions" onClose={() => undefined} onSubmit={soumettre} submitLabel="Exiger des actions" />)
    const d = screen.getByRole('alertdialog')
    expect(within(d).getByText('Votre clé de sécurité vous sera demandée pour confirmer ce geste.')).toBeTruthy()
    const b = within(d).getByRole('button', { name: /Exiger des actions/ }) as HTMLButtonElement
    expect(b.getAttribute('title')).toBe('Le motif est obligatoire (10 caractères au moins)')
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: '  Suspicion de fuite  ' } })
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).toHaveBeenCalledWith('Suspicion de fuite')
    rerender(<CrudDialog context="confirm" title="Désactiver" onClose={() => undefined} onSubmit={soumettre} submitLabel="Désactiver le compte" submitDisabled submitDisabledReason="Choisissez" />)
    expect(screen.getByRole('button', { name: 'Désactiver le compte' }).getAttribute('title')).toBe('Choisissez')
  })

  it('lecture : seul « Fermer » au pied, sans bouton principal ; fermée : rien n’est rendu', () => {
    const { rerender } = render(<CrudDialog context="detail" title="Mot de passe provisoire" onClose={() => undefined} submitLabel="Ignoré" inline><p>secret</p></CrudDialog>)
    const d = screen.getByRole('dialog')
    expect(within(d).getAllByRole('button').map((b) => b.textContent)).toEqual(['', 'Fermer'])
    fireEvent.submit(d.querySelector('form')!)
    rerender(<CrudDialog open={false} title="T" onClose={() => undefined} />)
    expect(screen.queryByRole('dialog')).toBeNull()
  })
})

type Inst = { nom: string; pays: string }
const descripteur: ResourceDescriptor<Inst> = {
  key: 'institution',
  label: { singular: 'institution', plural: 'institutions', gender: 'f' },
  idField: 'nom',
  titleField: 'nom',
  endpoints: { list: '', get: '' },
  permissions: { list: 'X', read: 'X' },
  actions: [],
  fields: [
    { name: 'nom', label: 'Nom', kind: 'text', inForm: true, required: true },
    { name: 'pays', label: 'Pays', kind: 'text', inForm: true },
  ],
}

describe('ResourceForm en dialogue', () => {
  it('formulaire dans un dialogue modal : titre du dialogue, soumission des valeurs, annulation', () => {
    const soumettre = vi.fn()
    const annuler = vi.fn()
    render(<ResourceForm descriptor={descripteur} presentation="dialog" mode="edit" initial={{ nom: 'Banque B' }} errors={{ nom: 'Obligatoire' }} onSubmit={soumettre} onCancel={annuler} />)
    const d = screen.getByRole('dialog', { name: 'Modifier une institution' })
    expect(within(d).queryByRole('heading', { level: 1 })).toBeNull()
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer les modifications' }))
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).toHaveBeenCalledWith({ nom: 'Banque B' })
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    expect(annuler).toHaveBeenCalled()
  })
  it('étapes : suivante puis précédente, soumission à la dernière', () => {
    const soumettre = vi.fn()
    render(<ResourceForm descriptor={descripteur} presentation="dialog" title="Prise de contact" submitLabel="Enregistrer" confirm="sign" submitting={false}
      steps={[{ label: 'Identité', fields: ['nom'] }, { label: 'Lieu', fields: ['pays'] }]} onSubmit={soumettre} />)
    const d = screen.getByRole('dialog', { name: 'Prise de contact' })
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).not.toHaveBeenCalled()
    expect(within(d).getByLabelText('Pays')).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Étape précédente' }))
    expect(within(d).getByLabelText(/Nom/)).toBeTruthy()
    fireEvent.submit(d.querySelector('form')!)
    fireEvent.submit(d.querySelector('form')!)
    expect(soumettre).toHaveBeenCalledTimes(1)
    fireEvent.keyDown(d, { key: 'Escape' })
  })
})

describe('StatCard cliquable et SideNav de la coquille', () => {
  it('carte activée à la souris et au clavier (Entrée, Espace), pas par une autre touche', () => {
    const clic = vi.fn()
    render(<StatCard label="Sans second facteur" value={3} onClick={clic} />)
    const c = screen.getByRole('button')
    fireEvent.click(c)
    fireEvent.keyDown(c, { key: 'Enter' })
    fireEvent.keyDown(c, { key: ' ' })
    fireEvent.keyDown(c, { key: 'a' })
    expect(clic).toHaveBeenCalledTimes(3)
  })
  it('navigation à la hauteur de son conteneur (valeur CSS) ; logo sans nom de couche', () => {
    render(<SideNav height="100%" logo="/logo.png" couche="des" groups={[{ items: [{ key: 'a', label: 'A', icon: 'gauge', href: '/a' }] }]} />)
    const nav = screen.getByRole('navigation') as HTMLElement
    expect(nav.style.height).toBe('100%')
    expect(within(nav).getByRole('img', { name: 'GIMAC' })).toBeTruthy()
    expect(nav.querySelector('.gm-sidenav-marque span')).toBeNull()
    expect(nav.textContent).not.toMatch(/Application du département/i)
  })
})

describe('QrCode', () => {
  it('image nommée, carré de modules avec zone de silence, taille par défaut ou fournie, recalcul au changement de contenu', () => {
    const { rerender } = render(<QrCode value="otpauth://totp/GIMAC:c.ndong?secret=JBSWY3DPEHPK3PXP" label="Code QR" data-testid="qr" />)
    const svg = screen.getByRole('img', { name: 'Code QR' })
    expect(svg.getAttribute('width')).toBe('192')
    const [, , n] = svg.getAttribute('viewBox')!.split(' ').map(Number)
    expect(n).toBeGreaterThanOrEqual(29)
    const trace = svg.querySelector('path')!.getAttribute('d')!
    expect(trace).toMatch(/^M\d+ \d+h1v1h-1z/)
    expect(trace).not.toMatch(/M[0-3] /)
    rerender(<QrCode value="autre contenu" label="Code QR" size={120} />)
    expect(screen.getByRole('img', { name: 'Code QR' }).getAttribute('width')).toBe('120')
    expect(screen.getByRole('img').querySelector('path')!.getAttribute('d')).not.toBe(trace)
  })
})
