import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { act } from 'react'
import { ApiError, SIGNAL, appliquerSignal, viderSignaux } from '@gimac/socle'
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
  viderSignaux()
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
    expect(await screen.findByRole('region', { name: 'Annuaire de la plateforme' })).toBeTruthy()
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
    // La pièce s'ouvre dans la fenêtre d'aperçu ; « Télécharger » l'enregistre.
    fireEvent.click(within(fil).getByRole('button', { name: 'Ouvrir rapport-essais-LIA-2026-030.pdf' }))
    const apercu = await screen.findByRole('dialog', { name: /rapport-essais-LIA-2026-030\.pdf/ })
    // PDF dessiné par pdf.js dans la fenêtre (D-73), sans cadre blob: soumis à la politique de sécurité.
    expect(await within(apercu).findByRole('document', { name: /rapport-essais-LIA-2026-030\.pdf/ })).toBeTruthy()
    expect(apercu.querySelector('iframe')).toBeNull()
    fireEvent.click(within(apercu).getByRole('button', { name: 'Télécharger' }))
    await waitFor(() => expect(createObjectURL).toHaveBeenCalled())
    fireEvent.click(within(apercu).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    // Le CSV s'affiche en texte.
    fireEvent.click(within(fil).getByRole('button', { name: 'Ouvrir journal-essais.csv' }))
    const csv = await screen.findByRole('dialog', { name: /journal-essais\.csv/ })
    expect(await within(csv).findByText(/retrait;échec;05/)).toBeTruthy()
    fireEvent.click(within(csv).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
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
  it('nouveau message : une personne choisie dans l’annuaire par son nom, liaison rattachée depuis l’adresse ; ouverture du fil créé', async () => {
    const s = services()
    page('/des/messagerie?nouveau=1&liaison=LIA-2026-012', { s })
    const panneau = await screen.findByRole('region', { name: 'Nouveau message' })
    expect(await within(panneau).findByText(/Liaison LIA-2026-012/)).toBeTruthy()
    // Pas de saisie avant le choix du destinataire.
    expect(within(panneau).queryByLabelText('Votre message')).toBeNull()
    fireEvent.change(within(panneau).getByLabelText('Rechercher une personne'), { target: { value: 'rachel' } })
    const annuaire = within(panneau).getByRole('list', { name: 'Annuaire de la plateforme' })
    await waitFor(() => expect(within(annuaire).getAllByRole('button')).toHaveLength(1), { timeout: 3000 })
    fireEvent.click(within(annuaire).getByRole('button', { name: /Rachel Biyong Mengue/ }))
    expect(within(panneau).getByText('Destinataire')).toBeTruthy()
    expect(within(panneau).getByText('Normes et Agréments', { exact: false })).toBeTruthy()
    // La liaison rattachée est citée d'emblée.
    expect(await within(panneau).findByText('# LIA-2026-012')).toBeTruthy()
    const fichier = new File(['plan'], 'plan-bascule.pdf', { type: 'application/pdf' })
    fireEvent.change(panneau.querySelector('input[type="file"]')!, { target: { files: [fichier] } })
    fireEvent.change(within(panneau).getByLabelText('Votre message'), { target: { value: 'Plan de bascule joint.' } })
    // D-74 : la pièce part aussitôt par morceaux ; « Envoyer » attend qu'elle soit prête.
    const envois = within(panneau).getByRole('list', { name: 'Pièces en cours d’envoi' })
    expect(within(envois).getByText(/^Envoi \d+ %$/)).toBeTruthy()
    expect((within(panneau).getByRole('button', { name: 'Envoyer' }) as HTMLButtonElement).disabled).toBe(true)
    expect(await within(envois).findByText('Prêt', {}, { timeout: 5000 })).toBeTruthy()
    fireEvent.click(within(panneau).getByRole('button', { name: 'Envoyer' }))
    const fil = await screen.findByRole('region', { name: 'Rachel Biyong Mengue' })
    expect(await within(fil).findByText('plan-bascule.pdf', { exact: false })).toBeTruthy()
    // Rattachée à une liaison, l'échange est un fil sans nom propre à cette liaison (le direct reste réservé aux échanges libres).
    const c = (await s.messagerie.conversations({})).contenu.find((x) => x.liaison?.liaisonId === 'LIA-2026-012')
    expect(c).toMatchObject({ titre: null, membres: [{ nom: 'Agent de test' }, { nom: 'Rachel Biyong Mengue' }] })
  })
  it('destinataire proposé par l’adresse ; retiré, l’annuaire revient ; annuler revient à la liste', async () => {
    page('/des/messagerie?nouveau=1&destinataire=u-sna')
    const panneau = await screen.findByRole('region', { name: 'Nouveau message' })
    expect(await within(panneau).findByText('Rachel Biyong Mengue')).toBeTruthy()
    expect(within(panneau).getByLabelText('Votre message')).toBeTruthy()
    fireEvent.click(within(panneau).getAllByRole('button', { name: 'Annuler' })[0])
    expect(await screen.findByText('Choisissez une conversation')).toBeTruthy()
  })
  it('nouveau groupe : nom et membres exigés, puis groupe créé avec son événement de création', async () => {
    const s = services()
    page('/des/messagerie', { s })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau groupe' }))
    const panneau = await screen.findByRole('region', { name: 'Nouveau groupe de discussion' })
    fireEvent.click(within(panneau).getByRole('button', { name: 'Créer le groupe' }))
    expect(await within(panneau).findByText('Donnez un nom au groupe.', { exact: false }).catch(() => within(panneau).getAllByRole('alert')[0])).toBeTruthy()
    fireEvent.change(within(panneau).getByLabelText(/Nom du groupe/), { target: { value: 'Cellule de bascule' } })
    const annuaire = within(panneau).getByRole('list', { name: 'Annuaire de la plateforme' })
    await waitFor(() => expect(within(annuaire).getAllByRole('button').length).toBeGreaterThan(2))
    const [a, b] = within(annuaire).getAllByRole('button')
    fireEvent.click(a)
    fireEvent.click(b)
    expect(a.getAttribute('aria-pressed')).toBe('true')
    expect(within(panneau).getByText('2 personnes choisies', { exact: false })).toBeTruthy()
    fireEvent.click(within(panneau).getByRole('button', { name: 'Créer le groupe' }))
    const fil = await screen.findByRole('region', { name: 'Cellule de bascule' })
    expect(await within(fil).findByText(/Vous avez créé le groupe/)).toBeTruthy()
    const c = (await s.messagerie.conversations({ recherche: 'Cellule' })).contenu[0]
    expect(c).toMatchObject({ type: 'GROUPE', titre: 'Cellule de bascule' })
    expect(c.membres).toHaveLength(3)
  })
  it('gestion du groupe par son administrateur : renommer, ajouter, retirer ; quitter', async () => {
    const s = services()
    const [g] = (await s.messagerie.conversations({})).contenu
    page(`/des/messagerie?conversation=${g.id}`, { s })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    fireEvent.click(within(fil).getByRole('button', { name: 'Membres et réglages' }))
    const d = await screen.findByRole('dialog')
    fireEvent.change(within(d).getByLabelText(/Nom du groupe/), { target: { value: 'Essais LIA-2026-030 (suite)' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Renommer' }))
    await waitFor(async () => expect((await s.messagerie.conversation(g.id)).titre).toBe('Essais LIA-2026-030 (suite)'))
    // Retrait d'un membre, après confirmation.
    const retirer = within(d).getAllByRole('button', { name: /^Retirer / })[0]
    const avant = (await s.messagerie.conversation(g.id)).membres.length
    fireEvent.click(retirer)
    const confirmation = await screen.findByRole('alertdialog').catch(() => screen.getAllByRole('dialog').at(-1)!)
    fireEvent.click(within(confirmation).getAllByRole('button', { name: /^Retirer / }).at(-1)!)
    await waitFor(async () => expect((await s.messagerie.conversation(g.id)).membres).toHaveLength(avant - 1))
    // Ajout depuis l'annuaire, sans les membres actuels.
    const annuaire = within(d).getByRole('list', { name: 'Annuaire de la plateforme' })
    await waitFor(() => expect(within(annuaire).getAllByRole('button').length).toBeGreaterThan(0))
    fireEvent.click(within(annuaire).getAllByRole('button')[0])
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter 1 membre' }))
    await waitFor(async () => expect((await s.messagerie.conversation(g.id)).membres).toHaveLength(avant))
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
    fireEvent.change(await screen.findByLabelText('Rechercher une personne'), { target: { value: 'Rachel' } })
    const personne = await screen.findByRole('button', { name: /Rachel Biyong Mengue/ })
    await waitFor(() => expect(personne.querySelector('.gm-avatar img')).toBeTruthy())
    fireEvent.click(personne)
    await waitFor(() => expect(document.querySelector('.gm-annuaire-destinataire .gm-avatar img')).toBeTruthy())
    expect(container).toBeTruthy()
  })
  it('interlocuteur : seulement pour un échange direct sans objet', async () => {
    const { interlocuteur } = await import('./MessageriePage')
    const c = { type: 'DIRECTE', titre: null, membres: [{ id: 'a', nom: 'A', fonction: null }, { id: 'b', nom: 'B', fonction: null }] } as unknown as Conversation
    expect(interlocuteur(c, 'a')?.id).toBe('b')
    expect(interlocuteur({ ...c, type: 'GROUPE' }, 'a')).toBeUndefined()
  })
  it('D-72 : présence et « écrit… » dans la liste, l’en-tête et sous le fil ; la frappe signale la saisie', async () => {
    const s = services()
    const signaler = vi.spyOn(s.messagerie, 'signalerSaisie')
    const { contenu } = await s.messagerie.conversations({})
    const directe = contenu.find((c) => c.type === 'DIRECTE' && !c.titre)!
    const autre = directe.membres.find((m) => m.id !== 'u-test')!
    const groupe = contenu.find((c) => c.type === 'GROUPE')!
    page(`/des/messagerie?conversation=${directe.id}`, { s })
    const fil = await screen.findByRole('region', { name: autre.nom })
    const liste = await screen.findByRole('list', { name: 'Conversations' })
    // Présence initiale (service), puis tenue à jour par le canal.
    act(() => void appliquerSignal({ type: SIGNAL.horsLigne, subject: autre.id, acteur: autre.id }))
    expect(await within(fil).findByText(/^Vu /)).toBeTruthy()
    const entree = () => within(liste).getAllByRole('button').find((b) => b.textContent?.includes(autre.nom))!
    expect(within(entree()).queryByRole('img', { name: 'En ligne' })).toBeNull()
    act(() => void appliquerSignal({ type: SIGNAL.enLigne, subject: autre.id, acteur: autre.id }))
    expect(await within(fil).findByText('En ligne')).toBeTruthy()
    expect(within(fil).getByRole('img', { name: 'En ligne' })).toBeTruthy()
    expect(within(entree()).getByRole('img', { name: 'En ligne' })).toBeTruthy()
    // « écrit… » : seul dans la liste pour un échange direct, avec le prénom sous le fil ; effacé par son message.
    act(() => void appliquerSignal({ type: SIGNAL.saisie, subject: directe.id, acteur: autre.id }))
    expect(await within(liste).findByText('écrit…')).toBeTruthy()
    expect(within(fil).getByText(`${autre.nom.split(' ')[0]} écrit…`)).toBeTruthy()
    act(() => void appliquerSignal({ type: SIGNAL.message, subject: directe.id, acteur: autre.id }))
    await waitFor(() => expect(within(fil).queryByText(/écrit…/)).toBeNull())
    // Groupe : prénoms de ceux qui écrivent, dans la liste.
    act(() => {
      groupe.membres.filter((m) => m.id !== 'u-test').slice(0, 2).forEach((m) => appliquerSignal({ type: SIGNAL.saisie, subject: groupe.id, acteur: m.id }))
    })
    expect(await within(liste).findByText(/écrivent…|écrit…/)).toBeTruthy()
    // Frappe : un signal, pas un par touche.
    const champ = within(fil).getByLabelText('Votre message')
    fireEvent.change(champ, { target: { value: 'B' } })
    fireEvent.change(champ, { target: { value: 'Bo' } })
    fireEvent.change(champ, { target: { value: '' } })
    expect(signaler).toHaveBeenCalledTimes(1)
    expect(signaler).toHaveBeenCalledWith(directe.id)
  })
  it('D-72 : groupe, nombre de membres en ligne dans l’en-tête', async () => {
    const s = services()
    const groupe = (await s.messagerie.conversations({})).contenu.find((c) => c.type === 'GROUPE')!
    page(`/des/messagerie?conversation=${groupe.id}`, { s })
    const fil = await screen.findByRole('region', { name: groupe.titre ?? undefined })
    const membre = groupe.membres.find((m) => m.id !== 'u-test')!
    act(() => void appliquerSignal({ type: SIGNAL.enLigne, subject: membre.id, acteur: membre.id }))
    expect(await within(fil).findByText(/\d en ligne/)).toBeTruthy()
  })
  it('D-74 : transférer une pièce vers une autre conversation partage son ticket, sans nouvel envoi', async () => {
    const s = services()
    const deposer = vi.spyOn(s.messagerie, 'deposer')
    const { contenu } = await s.messagerie.conversations({})
    const groupe = contenu.find((c) => c.type === 'GROUPE')!
    const directe = contenu.find((c) => c.type === 'DIRECTE' && !c.titre)!
    const autre = directe.membres.find((m) => m.id !== 'u-test')!
    page(`/des/messagerie?conversation=${groupe.id}`, { s })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    fireEvent.click(await within(fil).findByRole('button', { name: 'Transférer rapport-essais-LIA-2026-030.pdf' }))
    const dialogue = await screen.findByRole('dialog', { name: /Transférer la pièce jointe/ })
    const choix = await within(dialogue).findByRole('button', { name: new RegExp(autre.nom) })
    expect(within(dialogue).queryByRole('button', { name: /Essais de LIA-2026-030/ })).toBeNull()
    fireEvent.click(choix)
    const cible = await screen.findByRole('region', { name: autre.nom })
    expect(await within(cible).findByRole('button', { name: 'Ouvrir rapport-essais-LIA-2026-030.pdf' })).toBeTruthy()
    expect(deposer).not.toHaveBeenCalled()
    const source = (await s.messagerie.messages(groupe.id)).contenu.flatMap((m) => m.piecesJointes).find((x) => x.nom === 'rapport-essais-LIA-2026-030.pdf')!
    const copie = (await s.messagerie.messages(directe.id)).contenu.flatMap((m) => m.piecesJointes).find((x) => x.nom === 'rapport-essais-LIA-2026-030.pdf')!
    expect(copie.empreinte).toBe(source.empreinte)
  })
  it('D-74 : pièce volumineuse téléchargée par lien direct, sans aperçu ni chargement en mémoire', async () => {
    const s = services()
    const original = s.messagerie.messages.bind(s.messagerie)
    vi.spyOn(s.messagerie, 'messages').mockImplementation(async (cid, avant) => {
      const f = await original(cid, avant)
      return { ...f, contenu: f.contenu.map((m) => ({ ...m, piecesJointes: m.piecesJointes.map((p) => ({ ...p, taille: 4 * 1024 ** 3 })) })) }
    })
    vi.spyOn(s.messagerie, 'lienPiece').mockImplementation((id) => `/bff/des/collaboration/messagerie/pieces/${id}`)
    const telecharger = vi.spyOn(s.messagerie, 'telechargerPiece')
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    const groupe = (await s.messagerie.conversations({})).contenu.find((c) => c.type === 'GROUPE')!
    page(`/des/messagerie?conversation=${groupe.id}`, { s })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    expect((await within(fil).findAllByText(/4,0 Go/)).length).toBeGreaterThan(0)
    fireEvent.click(within(fil).getByRole('button', { name: 'Ouvrir rapport-essais-LIA-2026-030.pdf' }))
    expect(clic).toHaveBeenCalledTimes(1)
    expect((clic.mock.contexts[0] as HTMLAnchorElement).getAttribute('href')).toMatch(/^\/bff\/des\/collaboration\/messagerie\/pieces\//)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(telecharger).not.toHaveBeenCalled()
  })
  it('D-74 : pièce refusée (type, taille) signalée ; envoi en cours annulé', async () => {
    const s = services()
    const groupe = (await s.messagerie.conversations({})).contenu.find((c) => c.type === 'GROUPE')!
    page(`/des/messagerie?conversation=${groupe.id}`, { s })
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    const selecteur = fil.querySelector('input[type="file"]')!
    const lourd = new File(['x'], 'enorme.mp4', { type: 'video/mp4' })
    Object.defineProperty(lourd, 'size', { value: 21 * 1024 ** 3 })
    fireEvent.change(selecteur, { target: { files: [new File(['x'], 'outil.exe'), lourd, new File(['video'], 'atelier.mp4', { type: 'video/mp4' })] } })
    const envois = within(fil).getByRole('list', { name: 'Pièces en cours d’envoi' })
    expect(await within(envois).findByText('Ce type de fichier n’est pas admis.')).toBeTruthy()
    expect(within(envois).getByText('Fichier de plus de 20 Go')).toBeTruthy()
    fireEvent.click(within(envois).getByRole('button', { name: 'Annuler l’envoi de atelier.mp4' }))
    expect(within(envois).queryByText('atelier.mp4')).toBeNull()
    fireEvent.click(within(envois).getByRole('button', { name: 'Retirer outil.exe' }))
    fireEvent.click(within(envois).getByRole('button', { name: 'Retirer enorme.mp4' }))
    expect(within(fil).queryByRole('list', { name: 'Pièces en cours d’envoi' })).toBeNull()
  })
})
