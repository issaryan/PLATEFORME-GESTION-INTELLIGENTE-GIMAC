// Réponse à un message et mentions (D-82), aperçu des documents bureautiques (D-83), navigation réduite (D-84).
import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { useState } from 'react'
import {
  AttachmentViewer, familleBureautique, formatApercu, LangueProvider, membresProposes, mentionEnCours, MessageComposer, MessageThread, MultiSelect, SideNav,
  TAILLE_MAX_APERCU_BUREAUTIQUE, type MembreMention,
} from '../src'

afterEach(cleanup)
vi.mock('pdfjs-dist/legacy/build/pdf.worker.min.mjs?url', () => ({ default: '/assets/pdf.worker.mjs' }))
vi.mock('pdfjs-dist/legacy/build/pdf.mjs', () => ({
  GlobalWorkerOptions: { workerSrc: '' },
  getDocument: () => ({ destroy: () => undefined, promise: Promise.resolve({ numPages: 0, getPage: async () => undefined }) }),
}))

describe('MessageThread : citations, réponse, mise en évidence (D-82)', () => {
  it('citation cliquable, message supprimé, pièces, « Vous » ; bouton Répondre ; mention signalée', () => {
    const ouvrir = vi.fn()
    const repondre = vi.fn()
    render(
      <MessageThread
        titre="Essais"
        onRepondre={repondre}
        messages={[
          { id: 'm1', auteur: 'Rachel', quand: '09:10', contenu: 'Rapport joint' },
          { id: 'm2', auteur: 'Hervé', quand: '09:12', contenu: 'Bien noté', mentionneMoi: true, citation: { auteur: 'Rachel', extrait: 'Rapport joint', pieces: 2, onOuvrir: ouvrir } },
          { id: 'm3', auteur: 'Paul', quand: '09:13', contenu: 'Merci', mine: true, citation: { auteur: 'Paul', mine: true, extrait: '', pieces: 1 } },
          { id: 'm4', auteur: 'Hervé', quand: '09:14', contenu: 'Ancien', citation: { auteur: null, extrait: '', supprime: true, onOuvrir: ouvrir } },
          { id: 'e1', auteur: 'Paul', quand: '09:15', contenu: 'Paul a créé le groupe', evenement: true },
        ]}
      />,
    )
    const cite = screen.getByRole('button', { name: /En réponse à :\s*Rachel\s*Rapport joint/ })
    expect(cite.getAttribute('title')).toBe('Aller au message cité')
    fireEvent.click(cite)
    expect(ouvrir).toHaveBeenCalledTimes(1)
    // Message cité supprimé : ni auteur ni lien.
    expect(screen.getByText('Message supprimé').closest('button')).toBeNull()
    // Citation de son propre message, sans texte : « Vous » et le nombre de pièces.
    expect(screen.getByText('1 pièce(s) jointe(s)')).toBeTruthy()
    expect(screen.getAllByText('Vous').length).toBeGreaterThan(0)
    // Bulle qui concerne le lecteur : signalée et annoncée.
    const m2 = screen.getByText('Bien noté').closest('li')!
    expect(m2.classList.contains('gm-msg-mentionne')).toBe(true)
    expect(within(m2).getByRole('img', { name: 'Vous concerne' })).toBeTruthy()
    fireEvent.click(within(m2).getByRole('button', { name: 'Répondre à Hervé' }))
    expect(repondre).toHaveBeenCalledWith('m2')
    expect(screen.getByRole('button', { name: 'Répondre à mon message' })).toBeTruthy()
    // Pas de réponse à un événement de groupe.
    expect(within(screen.getByText('Paul a créé le groupe').closest('li')!).queryByRole('button')).toBeNull()
  })

  it('balayage vers la droite sur écran tactile : répondre ; geste vertical ignoré', () => {
    const repondre = vi.fn()
    render(<MessageThread titre="F" onRepondre={repondre} messages={[{ id: 'm1', auteur: 'A', quand: '1', contenu: 'Bonjour' }]} />)
    const li = screen.getByText('Bonjour').closest('li')!
    fireEvent.touchStart(li, { touches: [{ clientX: 10, clientY: 100 }] })
    fireEvent.touchEnd(li, { changedTouches: [{ clientX: 30, clientY: 100 }] })
    expect(repondre).not.toHaveBeenCalled()
    fireEvent.touchStart(li, { touches: [{ clientX: 10, clientY: 100 }] })
    fireEvent.touchEnd(li, { changedTouches: [{ clientX: 120, clientY: 180 }] })
    expect(repondre).not.toHaveBeenCalled()
    fireEvent.touchStart(li, { touches: [{ clientX: 10, clientY: 100 }] })
    fireEvent.touchEnd(li, { changedTouches: [{ clientX: 120, clientY: 110 }] })
    expect(repondre).toHaveBeenCalledWith('m1')
    // Fin de geste sans début connu : rien.
    fireEvent.touchEnd(li, { changedTouches: [{ clientX: 300, clientY: 110 }] })
    expect(repondre).toHaveBeenCalledTimes(1)
  })

  it('message mis en évidence : surligné et amené au centre ; annonce sous le fil', () => {
    const defiler = vi.fn()
    Element.prototype.scrollIntoView = defiler
    const messages = [{ id: 'm1', auteur: 'A', quand: '1', contenu: 'Un' }, { id: 'm2', auteur: 'B', quand: '2', contenu: 'Deux' }]
    const { rerender } = render(<MessageThread titre="F" messages={messages} />)
    expect(defiler).not.toHaveBeenCalled()
    rerender(<MessageThread titre="F" messages={messages} enEvidence="m1" annonce="Message plus ancien non chargé" />)
    expect(screen.getByText('Un').closest('li')!.classList.contains('gm-msg-evidence')).toBe(true)
    expect(defiler).toHaveBeenCalledWith({ block: 'center', behavior: 'smooth' })
    expect(screen.getByText('Message plus ancien non chargé')).toBeTruthy()
  })
})

describe('MessageComposer : réponse en cours et mentions (D-82)', () => {
  const membres: MembreMention[] = [
    { id: 'u1', nom: 'Aïcha Mahamat Nour', detail: 'Conductrice de projet' },
    { id: 'u2', nom: 'Hervé Kamga Tchoupo' },
    { id: 'u3', nom: 'Rachel Biyong Mengue', avatar: null },
  ]
  function Banc({ onMention = vi.fn(), onSend = vi.fn(), reponse }: { onMention?: (m: MembreMention) => void; onSend?: () => void; reponse?: { cle: string; auteur: string; extrait: string; onAnnuler: () => void } }) {
    const [v, setV] = useState('')
    return <MessageComposer value={v} onChange={setV} onSend={onSend} membres={membres} onMention={onMention} reponse={reponse} />
  }
  const champ = () => screen.getByLabelText('Votre message') as HTMLTextAreaElement
  const taper = (texte: string) => fireEvent.change(champ(), { target: { value: texte, selectionStart: texte.length } })

  it('fonctions pures : mention en cours, membres proposés sans accents ni casse', () => {
    expect(mentionEnCours('Bonjour @ai', 11)).toEqual({ debut: 8, requete: 'ai' })
    expect(mentionEnCours('@', 1)).toEqual({ debut: 0, requete: '' })
    expect(mentionEnCours('courriel@gimac', 14)).toBeNull()
    expect(mentionEnCours('Bonjour', 7)).toBeNull()
    expect(membresProposes(membres, 'aicha').map((m) => m.id)).toEqual(['u1'])
    expect(membresProposes(membres, '').length).toBe(3)
  })

  it('« @ » ouvre la liste filtrée ; flèches, Entrée insère « @Prénom Nom » et rend l’identifiant', async () => {
    const mention = vi.fn()
    const envoyer = vi.fn()
    render(<Banc onMention={mention} onSend={envoyer} />)
    expect(champ().getAttribute('aria-autocomplete')).toBe('list')
    expect(champ().getAttribute('placeholder')).toContain('@ pour mentionner')
    taper('Bonjour @')
    const liste = screen.getByRole('listbox', { name: 'Membres à mentionner' })
    expect(within(liste).getAllByRole('option')).toHaveLength(3)
    expect(screen.getByText(/3 membre\(s\) proposé\(s\)/)).toBeTruthy()
    expect(champ().getAttribute('aria-controls')).toBe(liste.id)
    taper('Bonjour @ou')
    // « ou » : Aïcha (Nour) et Hervé (Tchoupo) ; la flèche descend, puis remonte en boucle.
    expect(within(liste).getAllByRole('option')).toHaveLength(2)
    fireEvent.keyDown(champ(), { key: 'ArrowDown' })
    expect(within(liste).getAllByRole('option')[1].getAttribute('aria-selected')).toBe('true')
    expect(champ().getAttribute('aria-activedescendant')).toBe(within(liste).getAllByRole('option')[1].id)
    fireEvent.keyDown(champ(), { key: 'ArrowDown' })
    fireEvent.keyDown(champ(), { key: 'ArrowUp' })
    fireEvent.keyDown(champ(), { key: 'Enter' })
    expect(champ().value).toBe('Bonjour @Hervé Kamga Tchoupo ')
    expect(mention).toHaveBeenCalledWith(expect.objectContaining({ id: 'u2' }))
    expect(envoyer).not.toHaveBeenCalled()
    expect(screen.queryByRole('listbox')).toBeNull()
    await act(async () => { await new Promise((r) => setTimeout(r, 5)) })
    expect(champ().selectionStart).toBe('Bonjour @Hervé Kamga Tchoupo '.length)
    // Liste fermée : Entrée envoie.
    fireEvent.keyDown(champ(), { key: 'Enter' })
    expect(envoyer).toHaveBeenCalledTimes(1)
  })

  it('clic sur une personne, Tab, Échap ferme la liste sans annuler la réponse, aucun résultat', () => {
    const mention = vi.fn()
    const annuler = vi.fn()
    render(<Banc onMention={mention} reponse={{ cle: 'm9', auteur: 'Hervé', extrait: 'Attestation reçue', onAnnuler: annuler }} />)
    // La réponse reprend le focus.
    expect(document.activeElement).toBe(champ())
    expect(screen.getByRole('group', { name: 'Réponse à Hervé' })).toBeTruthy()
    taper('@he')
    fireEvent.mouseDown(screen.getByRole('option', { name: /Hervé Kamga Tchoupo/ }))
    expect(champ().value).toBe('@Hervé Kamga Tchoupo ')
    taper('@Hervé Kamga Tchoupo et @ai')
    fireEvent.keyDown(champ(), { key: 'Tab' })
    expect(mention).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'u1' }))
    taper('@zz')
    expect(screen.queryByRole('listbox')).toBeNull()
    taper('@')
    fireEvent.keyDown(champ(), { key: 'Escape' })
    expect(screen.queryByRole('listbox')).toBeNull()
    expect(annuler).not.toHaveBeenCalled()
    // Clic dans le texte : la mention sous le curseur est de nouveau proposée ; la perte du focus la ferme.
    fireEvent.click(champ())
    expect(screen.getByRole('listbox')).toBeTruthy()
    fireEvent.blur(champ())
    expect(screen.queryByRole('listbox')).toBeNull()
    // Échap sans liste : la réponse est annulée ; la croix aussi.
    fireEvent.keyDown(champ(), { key: 'Escape' })
    expect(annuler).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: 'Annuler la réponse' }))
    expect(annuler).toHaveBeenCalledTimes(2)
  })

  it('sans membres : ni liste ni autocomplétion', () => {
    render(<MessageComposer value="@a" onChange={() => undefined} onSend={() => undefined} />)
    fireEvent.change(champ(), { target: { value: '@ai', selectionStart: 3 } })
    expect(screen.queryByRole('listbox')).toBeNull()
    expect(champ().getAttribute('aria-autocomplete')).toBeNull()
  })
})

describe('AttachmentViewer : documents bureautiques (D-83)', () => {
  const base = { onTelecharger: vi.fn(), onClose: vi.fn() }
  it('formats et familles', () => {
    expect(formatApercu('application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'note.docx')).toBe('bureautique')
    expect(formatApercu('text/rtf', 'lettre.RTF')).toBe('bureautique')
    expect(formatApercu('application/octet-stream', 'plan.pptx')).toBe('bureautique')
    expect(formatApercu('application/octet-stream', 'sans-extension')).toBe('aucun')
    expect(familleBureautique('a.odt')).toBe('texte')
    expect(familleBureautique('a.odp')).toBe('presentation')
    expect(familleBureautique('a.xls')).toBe('tableur')
    expect(familleBureautique('a.zip')).toBeNull()
  })

  it('préparation, puis PDF converti ; pictogramme du type devant le titre', async () => {
    let fournir: (b: Blob) => void = () => undefined
    const convertir = vi.fn(() => new Promise<Blob>((ok) => { fournir = ok }))
    const source = new Blob(['PK'])
    render(<AttachmentViewer {...base} nom="plan.pptx" typeMime="application/octet-stream" blob={source} convertir={convertir} />)
    const d = screen.getByRole('dialog', { name: /plan\.pptx/ })
    expect(within(d).getByRole('status').textContent).toContain('Préparation de l’aperçu…')
    expect(convertir).toHaveBeenCalledWith(source, 'plan.pptx')
    expect(d.querySelector('h2 .gm-dialog-icone')).toBeTruthy()
    await act(async () => fournir(new Blob(['%PDF'], { type: 'application/pdf' })))
    expect(await within(d).findByRole('document', { name: 'Aperçu de plan.pptx' })).toBeTruthy()
    expect(within(d).getByRole('button', { name: 'Télécharger' })).toBeTruthy()
  })

  it.each([
    [413, undefined, 'Document trop volumineux pour l’aperçu (20 Mo au plus).'],
    [415, undefined, 'Ce format ne peut pas être prévisualisé.'],
    [422, undefined, 'Document illisible : fichier endommagé ou protégé par mot de passe.'],
    [429, { reessayerDans: 12.4 }, 'Trop d’aperçus demandés ; réessayez dans 13 s.'],
    [429, undefined, 'Trop d’aperçus demandés ; réessayez dans un instant.'],
    [503, undefined, 'Le service d’aperçu est momentanément indisponible ; réessayez plus tard.'],
    [403, undefined, 'Votre compte n’a pas accès à l’aperçu des documents.'],
    [0, undefined, 'L’aperçu n’a pas pu être préparé.'],
  ])('refus %i : cause affichée, téléchargement toujours possible', async (status, details, message) => {
    const convertir = vi.fn(() => Promise.reject(Object.assign(new Error('refus'), { status, details })))
    render(<AttachmentViewer {...base} nom="note.docx" typeMime="application/msword" blob={new Blob(['x'])} convertir={convertir} />)
    expect(await screen.findByText(message)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Télécharger' })).toBeTruthy()
  })

  it('au-delà de 20 Mo : refus avant tout envoi ; sans conversion fournie : invitation au téléchargement', () => {
    const convertir = vi.fn()
    const gros = { size: TAILLE_MAX_APERCU_BUREAUTIQUE + 1 } as Blob
    const { rerender } = render(<AttachmentViewer {...base} nom="tableau.xlsx" typeMime="application/octet-stream" blob={gros} convertir={convertir} />)
    expect(screen.getByText('Document trop volumineux pour l’aperçu (20 Mo au plus).')).toBeTruthy()
    expect(convertir).not.toHaveBeenCalled()
    rerender(<AttachmentViewer {...base} nom="tableau.xlsx" typeMime="application/octet-stream" blob={new Blob(['x'])} />)
    expect(screen.getByText('Aperçu indisponible pour ce format')).toBeTruthy()
  })

  it('fermeture pendant la conversion : le résultat tardif est ignoré', async () => {
    let fournir: (b: Blob) => void = () => undefined
    const convertir = vi.fn(() => new Promise<Blob>((ok) => { fournir = ok }))
    const { unmount } = render(<AttachmentViewer {...base} nom="a.odt" typeMime="application/vnd.oasis.opendocument.text" blob={new Blob(['x'])} convertir={convertir} />)
    unmount()
    await act(async () => fournir(new Blob(['%PDF'])))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('se traduit', async () => {
    render(<LangueProvider langue="es"><AttachmentViewer {...base} nom="a.docx" typeMime="x" blob={new Blob(['x'])} convertir={() => Promise.reject({ status: 503 })} /></LangueProvider>)
    await waitFor(() => expect(screen.getByText('El servicio de vista previa no está disponible por el momento; vuelva a intentarlo más tarde.')).toBeTruthy())
  })
})

describe('Navigation réduite et étiquettes tronquées (D-84, D-77)', () => {
  it('SideNav réduite : classe, libellés conservés pour les technologies d’assistance, infobulles', () => {
    render(<SideNav reduite current="msg" groups={[{ label: 'Travail', items: [{ key: 'msg', label: 'Messagerie', icon: 'messages-square', href: '/des/messagerie', badge: 3 }] }]} />)
    const nav = screen.getByRole('navigation', { name: 'Navigation principale' })
    expect(nav.classList.contains('gm-sidenav-reduite')).toBe(true)
    const lien = within(nav).getByRole('link', { name: /Messagerie/ })
    expect(lien.getAttribute('title')).toBe('Messagerie')
    expect(nav.querySelector('.gm-sidenav-group')!.getAttribute('aria-hidden')).toBe('true')
  })
  it('MultiSelect : libellé long tronqué, lu en entier dans l’infobulle', () => {
    const long = 'C1 · Vérification de la conformité du dossier technique de la liaison avant les essais'
    render(<MultiSelect label="Prédécesseurs" value={['C1']} options={[{ value: 'C1', label: long }]} />)
    expect(screen.getByTitle(long).classList.contains('gm-tag-texte')).toBe(true)
  })
})
