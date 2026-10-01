import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import MessageriePage, { CorpsMessage, titreConversation } from './MessageriePage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import type { Conversation } from '../../../modele/messagerie'
import { PERM } from '../../../permissions'

// Messagerie de travail (D-64) : liste, fil, nouveau message (annuaire, objet, liaison), étiquettes de
// liaison en liens, pièces jointes, non-lus, droits (lecture seule sans COL_MESSAGE_SEND), refus expliqués.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})
const LECTURE = [PERM.messageLire, PERM.liaisonLister, PERM.liaisonLire]
const ECRITURE = [...LECTURE, PERM.messageEnvoyer]
const services = () => servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-test', nom: 'Agent de test' } })
const page = (chemin = '/des/messagerie', o: { permissions?: string[]; s?: Services } = {}) =>
  rendrePage(<MessageriePage />, { chemin, motif: '/des/messagerie', services: o.s ?? services(), session: sessionTest(o.permissions ?? ECRITURE) })

describe('MessageriePage', () => {
  it('console (RSSI, super administrateur) : photos de la couche console, liaisons en texte, sans citation ni lien vers le DES', async () => {
    const s = services()
    const espion = vi.spyOn(s.parcours, 'listerLiaisons')
    const groupe = (await s.messagerie.conversations({})).contenu[0]
    rendrePage(<MessageriePage couche="console" />, {
      chemin: `/console/messagerie?conversation=${groupe.id}`, motif: '/console/messagerie', services: s, session: sessionTest([PERM.messageLire, PERM.messageEnvoyer]),
    })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    await waitFor(() => expect(within(fil).getAllByText(/LIA-2026-030/).length).toBeGreaterThan(1))
    expect(within(fil).queryAllByRole('link', { name: /LIA-2026/ })).toEqual([])
    expect(within(fil).queryByRole('link', { name: /Ouvrir la liaison/ })).toBeNull()
    expect(within(fil).queryByRole('button', { name: 'Citer une liaison' })).toBeNull()
    expect(within(fil).getByLabelText('Votre message')).toBeTruthy()
    // Nouveau message : aucune lecture des liaisons depuis la console.
    fireEvent.click(screen.getByRole('button', { name: 'Nouveau message' }))
    expect(await screen.findByText('Destinataires')).toBeTruthy()
    expect(espion).not.toHaveBeenCalled()
  })
  it('liste des conversations : la plus récente en tête, non-lus, liaison rattachée ; choix d’une conversation', async () => {
    page()
    const liste = await screen.findByRole('list', { name: 'Conversations' })
    const boutons = within(liste).getAllByRole('button')
    expect(boutons).toHaveLength(3)
    expect(boutons[0].textContent).toContain('Essais de LIA-2026-030')
    expect(boutons[0].textContent).toContain('Liaison LIA-2026-030')
    expect(within(liste).getByLabelText('2 non lus')).toBeTruthy()
    expect(screen.getByText('Choisissez une conversation')).toBeTruthy()
    // État vide sur le fond texturé du fil (motif de trame, D-66).
    expect(screen.getByText('Choisissez une conversation').closest('.gm-messagerie-fil')!.classList.contains('gm-trame-fil')).toBe(true)
    // Pagination toujours visible sous la liste.
    expect(screen.getByText(/Page 1 sur 1/)).toBeTruthy()
    fireEvent.click(boutons[0])
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    expect(within(fil).getByRole('link', { name: 'Ouvrir la liaison LIA-2026-030' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-030')
    expect(await within(fil).findByText('Rachel Biyong Mengue', { selector: 'strong' }, { timeout: 3000 }).catch(() => within(fil).getAllByText('Rachel Biyong Mengue')[0])).toBeTruthy()
  })
  it('fil : étiquettes en liens, pièce téléchargée, lecture marquée ; envoi d’un message avec une liaison citée', async () => {
    const s = services()
    const createObjectURL = vi.fn(() => 'blob:x')
    Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() })
    const conversations = await s.messagerie.conversations({})
    const groupe = conversations.contenu[0]
    page(`/des/messagerie?conversation=${groupe.id}`, { s })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    // Référence citée dans le texte et étiquette : liens vers le dossier de la liaison.
    await waitFor(() => expect(within(fil).getAllByRole('link', { name: /LIA-2026-030/ }).length).toBeGreaterThan(1))
    fireEvent.click(within(fil).getByRole('button', { name: 'Télécharger rapport-essais-LIA-2026-030.pdf' }))
    await waitFor(() => expect(createObjectURL).toHaveBeenCalled())
    // La conversation ouverte est marquée lue.
    await waitFor(async () => expect((await s.messagerie.nonLus()).total).toBe(1))
    // Citer une liaison, écrire, envoyer.
    fireEvent.click(within(fil).getByRole('button', { name: 'Citer une liaison' }))
    const citer = within(fil).getByRole('combobox', { name: 'Citer une liaison' })
    fireEvent.change(citer, { target: { value: 'LIA-2026-027' } })
    fireEvent.mouseDown(await within(fil).findByRole('option', { name: /LIA-2026-027/ }))
    expect(within(fil).getByText('# LIA-2026-027')).toBeTruthy()
    fireEvent.change(within(fil).getByLabelText('Votre message'), { target: { value: 'Point sur LIA-2026-027 demain.' } })
    fireEvent.click(within(fil).getByRole('button', { name: 'Envoyer' }))
    await waitFor(async () => {
      const derniers = (await s.messagerie.messages(groupe.id)).contenu
      expect(derniers[derniers.length - 1]).toMatchObject({ moi: true, contenu: 'Point sur LIA-2026-027 demain.', etiquettes: [{ reference: 'LIA-2026-027' }] })
    })
    await waitFor(() => expect((within(fil).getByLabelText('Votre message') as HTMLTextAreaElement).value).toBe(''))
    expect(await within(fil).findByText('Vous')).toBeTruthy()
  })
  it('nouveau message : destinataires de l’annuaire, objet, liaison rattachée depuis l’adresse ; ouverture du fil créé', async () => {
    const s = services()
    page('/des/messagerie?nouveau=1&liaison=LIA-2026-012', { s })
    const panneau = await screen.findByRole('region', { name: 'Nouveau message' })
    expect(await within(panneau).findByText(/Liaison LIA-2026-012/)).toBeTruthy()
    // La liaison rattachée est citée d'emblée.
    expect(await within(panneau).findByText('# LIA-2026-012')).toBeTruthy()
    const dest = within(panneau).getByRole('combobox', { name: 'Destinataires' })
    fireEvent.change(dest, { target: { value: 'rachel' } })
    fireEvent.mouseDown(await within(panneau).findByRole('option', { name: /Rachel Biyong Mengue/ }))
    expect(within(panneau).getByText(/Rachel Biyong Mengue · Normes et Agréments/)).toBeTruthy()
    fireEvent.change(within(panneau).getByLabelText('Objet'), { target: { value: 'Bascule de LIA-2026-012' } })
    const fichier = new File(['plan'], 'plan-bascule.pdf', { type: 'application/pdf' })
    fireEvent.change(panneau.querySelector('input[type="file"]')!, { target: { files: [fichier] } })
    expect(within(panneau).getByText('plan-bascule.pdf')).toBeTruthy()
    fireEvent.change(within(panneau).getByLabelText('Votre message'), { target: { value: 'Plan de bascule joint.' } })
    fireEvent.click(within(panneau).getByRole('button', { name: 'Envoyer' }))
    const fil = await screen.findByRole('region', { name: 'Bascule de LIA-2026-012' })
    expect(await within(fil).findByText('plan-bascule.pdf', { exact: false })).toBeTruthy()
    const c = (await s.messagerie.conversations({ recherche: 'bascule' })).contenu[0]
    expect(c).toMatchObject({ type: 'GROUPE', liaison: { liaisonId: 'LIA-2026-012' } })
  })
  it('refus du service expliqué ; annuler revient à la liste', async () => {
    page('/des/messagerie?nouveau=1')
    const panneau = await screen.findByRole('region', { name: 'Nouveau message' })
    fireEvent.change(within(panneau).getByLabelText('Votre message'), { target: { value: 'Sans destinataire' } })
    fireEvent.click(within(panneau).getByRole('button', { name: 'Envoyer' }))
    expect(await within(panneau).findByText('Choisissez au moins un destinataire.')).toBeTruthy()
    fireEvent.click(within(panneau).getByRole('button', { name: 'Annuler' }))
    expect(await screen.findByText('Choisissez une conversation')).toBeTruthy()
  })
  it('lecture seule : ni « Nouveau message » ni zone de saisie ; ?nouveau=1 ignoré', async () => {
    const s = services()
    const [c] = (await s.messagerie.conversations({})).contenu
    page(`/des/messagerie?conversation=${c.id}`, { permissions: LECTURE, s })
    await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    expect(screen.queryByRole('button', { name: 'Nouveau message' })).toBeNull()
    expect(screen.queryByLabelText('Votre message')).toBeNull()
    cleanup()
    page('/des/messagerie?nouveau=1', { permissions: LECTURE })
    expect(await screen.findByText('Choisissez une conversation')).toBeTruthy()
  })
  it('recherche dans les conversations ; aucune conversation ; conversation inconnue ; erreur de liste', async () => {
    page()
    fireEvent.change(await screen.findByLabelText('Rechercher une conversation'), { target: { value: 'pci' } })
    await waitFor(() => expect(within(screen.getByRole('list', { name: 'Conversations' })).getAllByRole('button')).toHaveLength(1), { timeout: 3000 })
    cleanup()
    const s = services()
    const vide = { ...s, messagerie: { ...s.messagerie, conversations: async () => ({ contenu: [], total: 0, page: 0, taille: 20 }) } }
    page('/des/messagerie', { s: vide })
    expect((await screen.findAllByText('Aucune conversation')).length).toBeGreaterThan(0)
    cleanup()
    page('/des/messagerie?conversation=CONV-9999')
    expect(await screen.findByText('Cette conversation n’existe pas ou ne vous est pas ouverte.')).toBeTruthy()
    cleanup()
    const panne = { ...s, messagerie: { ...s.messagerie, conversations: async () => { throw new ApiError(503, 'X', 'Service indisponible', 'c') } } }
    page('/des/messagerie', { s: panne })
    expect(await screen.findByText('Service indisponible')).toBeTruthy()
  })
})

describe('titreConversation et CorpsMessage', () => {
  const base: Conversation = {
    id: 'c', type: 'DIRECTE', titre: null, liaison: null, nonLus: 0, misAJourLe: '', creeeLe: '', dernierMessage: null,
    membres: [{ id: 'moi', nom: 'Moi', fonction: null }, { id: 'a', nom: 'Aïcha', fonction: null }],
  }
  it('objet, sinon l’autre membre, sinon les membres', () => {
    expect(titreConversation({ ...base, titre: 'Objet' }, 'moi')).toBe('Objet')
    expect(titreConversation(base, 'moi')).toBe('Aïcha')
    expect(titreConversation({ ...base, membres: [base.membres[0]] }, 'moi')).toBe('Moi')
  })
  it('références citées : lien vers le dossier si étiquetées, vers la recherche sinon', () => {
    render(<CorpsMessage contenu="Voir LIA-2026-027 et LIA-2026-099." etiquettes={[{ liaisonId: 'uuid-27', reference: 'LIA-2026-027' }]} />)
    expect(screen.getByRole('link', { name: 'LIA-2026-027' }).getAttribute('href')).toBe('/des/parcours/uuid-27')
    expect(screen.getByRole('link', { name: 'LIA-2026-099' }).getAttribute('href')).toBe('/des/parcours?q=LIA-2026-099')
  })
})

describe('MessageriePage : photos de profil (docs/lot1/mon-compte.md §4)', () => {
  it('conversation directe illustrée par la photo de l’interlocuteur, auteurs du fil, destinataires proposés et choisis', async () => {
    const s = services()
    const { container } = page('/des/messagerie', { s })
    const liste = await screen.findByRole('list', { name: 'Conversations' })
    // Échange direct avec Hervé Kamga (photo de démonstration) ; groupe : pictogramme.
    await waitFor(() => expect(within(liste).getByRole('img', { name: 'Hervé Kamga Tchoupo' }).querySelector('img')).toBeTruthy())
    fireEvent.click(within(liste).getAllByRole('button')[0])
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    await waitFor(() => expect(fil.querySelectorAll('.gm-msg-meta .gm-avatar img').length).toBeGreaterThan(0))
    cleanup()
    page('/des/messagerie?nouveau=1', { s })
    const champ = await screen.findByRole('combobox', { name: 'Destinataires' })
    fireEvent.focus(champ)
    fireEvent.change(champ, { target: { value: 'Rachel' } })
    const option = await screen.findByRole('option', { name: /Rachel Biyong Mengue/ })
    expect(option.querySelector('.gm-avatar img')).toBeTruthy()
    fireEvent.mouseDown(option)
    await waitFor(() => expect(document.querySelector('.gm-tag-personne img')).toBeTruthy())
    expect(container).toBeTruthy()
  })
  it('interlocuteur : seulement pour un échange direct sans objet', async () => {
    const { interlocuteur } = await import('./MessageriePage')
    const c = { type: 'DIRECTE', titre: null, membres: [{ id: 'a', nom: 'A', fonction: null }, { id: 'b', nom: 'B', fonction: null }] } as unknown as Conversation
    expect(interlocuteur(c, 'a')?.id).toBe('b')
    expect(interlocuteur({ ...c, type: 'GROUPE' }, 'a')).toBeUndefined()
  })
})
