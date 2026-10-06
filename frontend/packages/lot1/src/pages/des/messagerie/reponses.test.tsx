import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { ContexteBarreLaterale, viderSignaux } from '@gimac/socle'
import MessageriePage, { CorpsMessage, LARGEUR_LISTE } from './MessageriePage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PERM } from '../../../permissions'

// Réponse à un message et mentions (D-82), aperçu bureautique depuis le fil (D-83), largeur du fil (D-84).
beforeEach(() => {
  try {
    window.localStorage.clear()
  } catch {
    // stockage indisponible
  }
  Element.prototype.scrollIntoView = vi.fn()
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  viderSignaux()
})
const ECRITURE = [PERM.messageLire, PERM.liaisonLister, PERM.liaisonLire, PERM.messageEnvoyer]
const services = () => servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-test', nom: 'Agent de test' } })
const page = (chemin = '/des/messagerie', s: Services = services()) =>
  rendrePage(<MessageriePage />, { chemin, motif: '/des/messagerie', services: s, session: sessionTest(ECRITURE) })
const premiere = async (s: ReturnType<typeof services>) => (await s.messagerie.conversations({})).contenu

describe('CorpsMessage : mentions mises en évidence sans HTML', () => {
  it('mention, la sienne, liaison citée, texte brut conservé', () => {
    render(
      <CorpsMessage
        contenu="@Paul Mbarga et @Agent de test <b>voir</b> LIA-2026-030 @Inconnu"
        etiquettes={[]}
        mentions={[{ id: 'u-1', nom: 'Paul Mbarga' }, { id: 'u-test', nom: 'Agent de test' }]}
        moi="u-test"
      />,
    )
    expect(screen.getByText('@Paul Mbarga').className).toBe('gm-mention')
    expect(screen.getByText('@Agent de test').className).toBe('gm-mention gm-mention-moi')
    expect(screen.getByText(/<b>voir<\/b>/)).toBeTruthy()
    expect(document.querySelector('b')).toBeNull()
    expect(screen.getByRole('link', { name: 'LIA-2026-030' })).toBeTruthy()
    expect(screen.queryByText('@Inconnu', { selector: '.gm-mention' })).toBeNull()
  })
})

describe('MessageriePage : répondre et mentionner (D-82)', () => {
  it('citation dans la bulle, réponse en cours, mention choisie ; corps envoyé', async () => {
    const s = services()
    const envoyer = vi.spyOn(s.messagerie, 'envoyer')
    const [groupe] = await premiere(s)
    page(`/des/messagerie?conversation=${groupe.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    // Message du jeu de démonstration : réponse au rapport des essais, avec une mention de l'utilisateur.
    const reponse = (await within(fil).findByText('@Agent de test')).closest('li')!
    expect(reponse.classList.contains('gm-msg-mentionne')).toBe(true)
    expect(within(reponse).getByRole('button', { name: /En réponse à :\s*Rachel Biyong Mengue/ })).toBeTruthy()
    // Citation ouverte : le message d'origine est mis en évidence.
    fireEvent.click(within(reponse).getByRole('button', { name: /En réponse à/ }))
    await waitFor(() => expect(fil.querySelector('.gm-msg-evidence')!.textContent).toContain('Les essais sont suspendus'))
    // Répondre au message du RSSI.
    const herve = within(fil).getAllByText(/je prends contact/)[0].closest('li')!
    fireEvent.click(within(herve).getByRole('button', { name: 'Répondre à Hervé Kamga Tchoupo' }))
    const encours = within(fil).getByRole('group', { name: 'Réponse à Hervé Kamga Tchoupo' })
    expect(encours.textContent).toContain('je prends contact')
    const champ = within(fil).getByLabelText('Votre message') as HTMLTextAreaElement
    fireEvent.change(champ, { target: { value: 'Merci @her', selectionStart: 10 } })
    fireEvent.keyDown(champ, { key: 'Enter' })
    expect(champ.value).toBe('Merci @Hervé Kamga Tchoupo ')
    // Mention retenue tant que « @Nom » figure au texte.
    fireEvent.change(champ, { target: { value: 'Merci @Hervé Kamga Tchoupo, je relis. @ra', selectionStart: 41 } })
    fireEvent.keyDown(champ, { key: 'Escape' })
    expect(within(fil).getByRole('group', { name: 'Réponse à Hervé Kamga Tchoupo' })).toBeTruthy()
    fireEvent.change(champ, { target: { value: 'Merci @Hervé Kamga Tchoupo, je relis.', selectionStart: 37 } })
    fireEvent.click(within(fil).getByRole('button', { name: 'Envoyer' }))
    await waitFor(() => expect(envoyer).toHaveBeenCalled())
    expect(envoyer.mock.calls[0][1]).toMatchObject({ contenu: 'Merci @Hervé Kamga Tchoupo, je relis.', reponseA: expect.stringMatching(/^MSG-/), mentions: ['u-rssi'] })
    // Envoyé : la réponse en cours disparaît, la bulle porte la citation.
    await waitFor(() => expect(within(fil).queryByRole('group', { name: /Réponse à/ })).toBeNull())
    const envoye = (await within(fil).findByText(/je relis\./)).closest('li')!
    expect(within(envoye).getByRole('button', { name: /En réponse à :\s*Hervé Kamga Tchoupo/ })).toBeTruthy()
  })

  it('annuler une réponse par la croix ; message cité supprimé ; mention refusée traduite', async () => {
    const s = services()
    const conv = await premiere(s)
    const directe = conv.find((c) => c.membres.some((m) => m.id === 'u-rssi') && c.type === 'DIRECTE')!
    vi.spyOn(s.messagerie, 'envoyer').mockRejectedValueOnce(Object.assign(new Error('x'), { status: 422, code: 'COL_MENTION_INVALIDE', correlationId: 'c' }))
    page(`/des/messagerie?conversation=${directe.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Hervé Kamga Tchoupo' })
    expect(await within(fil).findByText('Message supprimé')).toBeTruthy()
    const li = within(fil).getByText('Attestation reçue et en cours de vérification.').closest('li')!
    fireEvent.click(within(li).getByRole('button', { name: /^Répondre à/ }))
    fireEvent.click(within(fil).getByRole('button', { name: 'Annuler la réponse' }))
    expect(within(fil).queryByRole('group', { name: /Réponse à/ })).toBeNull()
    fireEvent.change(within(fil).getByLabelText('Votre message'), { target: { value: 'Bonjour', selectionStart: 7 } })
    fireEvent.click(within(fil).getByRole('button', { name: 'Envoyer' }))
    expect(await within(fil).findByText('Une personne mentionnée ne fait pas partie de la conversation.')).toBeTruthy()
  })

  it('lien d’une notification : message chargé puis mis en évidence ; introuvable : « Message plus ancien non chargé »', async () => {
    const s = services()
    const [groupe] = await premiere(s)
    const cible = (await s.messagerie.messages(groupe.id)).contenu[1].id
    const r = page(`/des/messagerie?conversation=${groupe.id}&message=${cible}`, s)
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    await waitFor(() => expect(fil.querySelector('.gm-msg-evidence')).toBeTruthy())
    r.unmount()
    vi.useFakeTimers({ shouldAdvanceTime: true })
    try {
      page(`/des/messagerie?conversation=${groupe.id}&message=MSG-ABSENT`, s)
      expect(await screen.findByText('Message plus ancien non chargé')).toBeTruthy()
      await act(async () => { vi.advanceTimersByTime(4_500) })
      await waitFor(() => expect(screen.queryByText('Message plus ancien non chargé')).toBeNull())
    } finally {
      vi.useRealTimers()
    }
  })

  it('fil long : pages plus anciennes chargées pour retrouver le message cité', async () => {
    const s = services()
    const [groupe] = await premiere(s)
    const vieux = (await s.messagerie.messages(groupe.id)).contenu[1]
    for (let i = 0; i < 55; i++) await s.messagerie.envoyer(groupe.id, { contenu: `Point ${i}`, etiquettes: [], fichiers: [] })
    const lire = vi.spyOn(s.messagerie, 'messages')
    page(`/des/messagerie?conversation=${groupe.id}&message=${vieux.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    await waitFor(() => expect(fil.querySelector('.gm-msg-evidence')).toBeTruthy())
    expect(lire.mock.calls.some((c) => c[1])).toBe(true)
  })
})

describe('MessageriePage : aperçu bureautique depuis le fil (D-83)', () => {
  it('docx converti en PDF par le service (pictogramme Word), refus affiché avec Télécharger', async () => {
    const s = services()
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() })
    const convertir = vi.spyOn(s.apercu!, 'convertir')
    const directe = (await premiere(s)).find((c) => c.membres.some((m) => m.id === 'u-rssi') && c.type === 'DIRECTE')!
    page(`/des/messagerie?conversation=${directe.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Hervé Kamga Tchoupo' })
    fireEvent.click(await within(fil).findByRole('button', { name: 'Ouvrir note-attestations-PCI-DSS.docx' }))
    const d = await screen.findByRole('dialog', { name: /note-attestations-PCI-DSS\.docx/ })
    await waitFor(() => expect(convertir).toHaveBeenCalledWith(expect.anything(), 'note-attestations-PCI-DSS.docx'))
    expect(d.querySelector('.gm-dialog-icone')).toBeTruthy()
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    convertir.mockRejectedValueOnce(Object.assign(new Error('x'), { status: 503 }))
    fireEvent.click(within(fil).getByRole('button', { name: 'Ouvrir revue-securite-LIA-2026-019.pptx' }))
    const d2 = await screen.findByRole('dialog', { name: /revue-securite/ })
    expect(await within(d2).findByText('Le service d’aperçu est momentanément indisponible ; réessayez plus tard.')).toBeTruthy()
    expect(within(d2).getByRole('button', { name: 'Télécharger' })).toBeTruthy()
  })
})

describe('MessageriePage : largeur du fil (D-84)', () => {
  const reduire = vi.fn()
  const avecBarre = (s: Services, chemin: string, reduite = false) =>
    rendrePage(<ContexteBarreLaterale.Provider value={{ reduite, reduire, active: true }}><MessageriePage /></ContexteBarreLaterale.Provider>, { chemin, motif: '/des/messagerie', services: s, session: sessionTest(ECRITURE) })

  it('séparateur : clavier, Origine, Fin, double clic, glissement ; largeur gardée', async () => {
    reduire.mockClear()
    const s = services()
    avecBarre(s, '/des/messagerie')
    const sep = await screen.findByRole('separator', { name: 'Largeur de la liste des conversations' })
    const grille = sep.parentElement!
    expect(sep.getAttribute('aria-valuenow')).toBe(String(LARGEUR_LISTE.defaut))
    expect(sep.getAttribute('aria-orientation')).toBe('vertical')
    fireEvent.keyDown(sep, { key: 'ArrowRight' })
    expect(sep.getAttribute('aria-valuenow')).toBe('336')
    fireEvent.keyDown(sep, { key: 'ArrowLeft' })
    fireEvent.keyDown(sep, { key: 'ArrowLeft' })
    expect(sep.getAttribute('aria-valuenow')).toBe('304')
    fireEvent.keyDown(sep, { key: 'End' })
    expect(sep.getAttribute('aria-valuenow')).toBe('480')
    fireEvent.keyDown(sep, { key: 'ArrowRight' })
    expect(sep.getAttribute('aria-valuenow')).toBe('480')
    fireEvent.keyDown(sep, { key: 'Home' })
    expect(sep.getAttribute('aria-valuenow')).toBe('240')
    fireEvent.keyDown(sep, { key: 'a' })
    expect(grille.style.getPropertyValue('--gm-messagerie-liste')).toBe('240px')
    fireEvent.doubleClick(sep)
    expect(sep.getAttribute('aria-valuenow')).toBe('320')
    fireEvent.pointerMove(sep, { clientX: 500 })
    fireEvent.pointerDown(sep, { clientX: 100, pointerId: 1 })
    expect(sep.classList.contains('gm-glisse')).toBe(true)
    fireEvent.pointerMove(sep, { clientX: 160, pointerId: 1 })
    expect(sep.getAttribute('aria-valuenow')).toBe('380')
    fireEvent.pointerUp(sep, { clientX: 160, pointerId: 1 })
    fireEvent.pointerUp(sep, { clientX: 160, pointerId: 1 })
    expect(window.localStorage.getItem('gm.messagerie.largeurListe')).toBe('380')
    cleanup()
    avecBarre(services(), '/des/messagerie')
    expect((await screen.findByRole('separator')).getAttribute('aria-valuenow')).toBe('380')
  })

  it('liste repliée depuis l’en-tête du fil, rouverte ; « Élargir » réduit la barre latérale puis la rétablit', async () => {
    reduire.mockClear()
    const s = services()
    const [groupe] = await premiere(s)
    const r = avecBarre(s, `/des/messagerie?conversation=${groupe.id}`)
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    fireEvent.click(within(fil).getByRole('button', { name: 'Masquer la liste des conversations' }))
    expect(document.querySelector('.gm-messagerie-repliee')).toBeTruthy()
    expect(window.localStorage.getItem('gm.messagerie.listeRepliee')).toBe('1')
    fireEvent.click(within(fil).getByRole('button', { name: 'Afficher la liste des conversations' }))
    expect(document.querySelector('.gm-messagerie-repliee')).toBeNull()
    expect(reduire).toHaveBeenLastCalledWith(false)
    fireEvent.click(screen.getByRole('button', { name: 'Élargir' }))
    expect(reduire).toHaveBeenLastCalledWith(true)
    expect(window.localStorage.getItem('gm.messagerie.elargie')).toBe('1')
    fireEvent.click(screen.getByRole('button', { name: 'Rétablir la barre latérale' }))
    expect(reduire).toHaveBeenLastCalledWith(false)
    fireEvent.click(screen.getByRole('button', { name: 'Élargir' }))
    r.unmount()
    // En quittant la messagerie : la barre revient à son état précédent.
    expect(reduire).toHaveBeenLastCalledWith(false)
    // Préférence gardée : à la visite suivante, la barre est de nouveau réduite.
    reduire.mockClear()
    avecBarre(services(), '/des/messagerie?nouveau=1')
    expect(await screen.findByRole('button', { name: 'Rétablir la barre latérale' })).toBeTruthy()
    expect(reduire).toHaveBeenCalledWith(true)
    fireEvent.click(within(screen.getByRole('region', { name: 'Nouveau message' })).getByRole('button', { name: 'Masquer la liste des conversations' }))
    expect(document.querySelector('.gm-messagerie-repliee')).toBeTruthy()
  })

  it('écran étroit (moins de 961 px) ou stockage refusé : comportement d’origine', async () => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: vi.fn(), removeEventListener: vi.fn() }))
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('refusé') })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('refusé') })
    const s = services()
    const [groupe] = await premiere(s)
    rendrePage(<MessageriePage />, { chemin: `/des/messagerie?conversation=${groupe.id}`, motif: '/des/messagerie', services: s, session: sessionTest(ECRITURE) })
    await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    expect(screen.queryByRole('separator')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Masquer la liste des conversations' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Élargir' })).toBeNull()
  })

  it('NouveauGroupe : commande de repli dans l’en-tête', async () => {
    avecBarre(services(), '/des/messagerie?nouveau=groupe')
    const panneau = await screen.findByRole('region', { name: 'Nouveau groupe de discussion' })
    fireEvent.click(within(panneau).getByRole('button', { name: 'Masquer la liste des conversations' }))
    expect(document.querySelector('.gm-messagerie-repliee')).toBeTruthy()
  })
})
