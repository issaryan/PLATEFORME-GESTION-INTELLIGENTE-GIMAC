import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError, diffuserAccuseLecture, viderSignaux } from '@gimac/socle'
import MessageriePage from './MessageriePage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import { notificationsDemo, type DemoNotifications } from '../../../services/notifications'
import type { Services } from '../../../services/ports'
import { PERM } from '../../../permissions'

// Accusés de lecture (D-88, docs/lot1/accuses-lecture.md) : coches sous mes messages, détail « Lu par » d'un
// message de groupe (heure inconnue comprise), mise à jour en direct par l'événement poussé, liste des conversations.
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
  viderSignaux()
})
const ECRITURE = [PERM.messageLire, PERM.liaisonLister, PERM.liaisonLire, PERM.messageEnvoyer]
const services = () => servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-test', nom: 'Agent de test' } })
const page = (chemin: string, s: Services) => rendrePage(<MessageriePage />, { chemin, motif: '/des/messagerie', services: s, session: sessionTest(ECRITURE) })
const refus = (code: string) => expect.objectContaining({ code })

describe('messagerie simulée : accusés de lecture', () => {
  it('états ENVOYE, LU_PAR_CERTAINS, LU ; détail « Lu par » ; liste ; 404 indistinct', async () => {
    const s = services()
    const [groupe, direct] = (await s.messagerie.conversations({})).contenu
    const fil = (await s.messagerie.messages(groupe.id)).contenu
    // Messages des autres et événements : pas d'accusé.
    expect(fil.filter((m) => !m.moi || m.evenement).every((m) => m.lecture === undefined)).toBe(true)
    const mien = fil.find((m) => m.moi && !m.evenement)!
    expect(mien.lecture).toEqual({ etat: 'LU_PAR_CERTAINS', lus: 2, destinataires: 3 })
    const lectures = await s.messagerie.lectures(groupe.id, mien.id)
    expect(lectures.map((l) => [l.membre.nom, l.lu, l.luLe === null])).toEqual([
      ['Rachel Biyong Mengue', true, false], ['Hervé Kamga Tchoupo', true, true], ['Aïcha Mahamat Nour', false, true],
    ])
    // Dernier message de l'échange direct : celui d'Aïcha, sans accusé ; le mien est lu.
    expect(direct.dernierMessage?.lecture).toBeUndefined()
    const filDirect = (await s.messagerie.messages(direct.id)).contenu
    expect(filDirect.find((m) => m.moi)!.lecture).toEqual({ etat: 'LU', lus: 1, destinataires: 1 })
    // Envoi : ENVOYE, aussi dans la liste ; puis l'autre membre lit (accusé diffusé une seule fois).
    const envoye = await s.messagerie.envoyer(direct.id, { contenu: 'Bien noté.', etiquettes: [], fichiers: [] })
    expect(envoye.lecture).toEqual({ etat: 'ENVOYE', lus: 0, destinataires: 1 })
    expect((await s.messagerie.conversation(direct.id)).dernierMessage?.lecture?.etat).toBe('ENVOYE')
    const accuse = s.messagerie.lireDemo({ conversationId: direct.id })
    expect(accuse).toEqual({ conversationId: direct.id, lecteurId: 'U-201', luJusquA: envoye.envoyeLe })
    expect(s.messagerie.lireDemo({ conversationId: direct.id })).toBeNull()
    expect((await s.messagerie.conversation(direct.id)).dernierMessage?.lecture).toEqual({ etat: 'LU', lus: 1, destinataires: 1 })
    // Message d'un autre, inconnu, ou conversation étrangère : même refus.
    await expect(s.messagerie.lectures(groupe.id, fil[1].id)).rejects.toEqual(refus('COL_MESSAGE_NOT_FOUND'))
    await expect(s.messagerie.lectures(groupe.id, 'MSG-X')).rejects.toEqual(refus('COL_MESSAGE_NOT_FOUND'))
    await expect(s.messagerie.lectures('CONV-X', mien.id)).rejects.toBeInstanceOf(ApiError)
    // Membre ajouté après l'envoi : ne compte pas.
    await s.messagerie.ajouterMembres(groupe.id, ['u-dg'])
    expect((await s.messagerie.messages(groupe.id)).contenu.find((m) => m.id === mien.id)!.lecture).toEqual({ etat: 'LU_PAR_CERTAINS', lus: 2, destinataires: 3 })
    // Tous lisent : LU ; lecteur explicite inconnu ou moi : rien.
    expect(s.messagerie.lireDemo({ conversationId: groupe.id, lecteurId: 'U-201' })).not.toBeNull()
    expect(s.messagerie.lireDemo({ conversationId: groupe.id, lecteurId: 'u-test' })).toBeNull()
    expect(s.messagerie.lireDemo({ conversationId: groupe.id, lecteurId: 'u-inconnu' })).toBeNull()
    expect((await s.messagerie.messages(groupe.id)).contenu.find((m) => m.id === mien.id)!.lecture?.etat).toBe('LU')
  })

  it('pont de démonstration : lireMessages diffuse l’accusé', () => {
    const s = services()
    notificationsDemo(s, { moi: 'u-test' })
    const demo = (window as unknown as { gmDemoNotifications: DemoNotifications }).gmDemoNotifications
    expect(demo.lireMessages()).toBe(false)
    demo.recevoirMessage({})
    expect(demo.lireMessages()).toBe(false)
  })
})

describe('MessageriePage : coches et « Lu par »', () => {
  it('groupe : « Lu par 2 sur 3 », détail au clic avec heure inconnue ; relu à l’accusé poussé', async () => {
    const s = services()
    const [groupe] = (await s.messagerie.conversations({})).contenu
    page(`/des/messagerie?conversation=${groupe.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Essais de LIA-2026-030' })
    const bouton = await within(fil).findByRole('button', { name: 'Lu par 2 sur 3, voir qui l’a lu' })
    expect(bouton.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(bouton)
    const detail = await within(fil).findByRole('dialog', { name: 'Lu par' })
    expect(await within(detail).findByText('Rachel Biyong Mengue')).toBeTruthy()
    expect(within(detail).getByText('heure inconnue')).toBeTruthy()
    expect(within(within(detail).getByRole('list', { name: 'Pas encore lu' })).getByText('Aïcha Mahamat Nour')).toBeTruthy()
    // Échap ferme ; le survol rouvre, la sortie referme.
    fireEvent.keyDown(bouton, { key: 'Escape' })
    expect(within(fil).queryByRole('dialog', { name: 'Lu par' })).toBeNull()
    fireEvent.mouseEnter(bouton.parentElement!)
    expect(within(fil).getByRole('dialog', { name: 'Lu par' })).toBeTruthy()
    // Aïcha lit : le fil et le détail ouvert sont relus.
    act(() => {
      s.messagerie.lireDemo({ conversationId: groupe.id, lecteurId: 'U-201' })
    })
    expect(await within(fil).findByRole('button', { name: 'Lu, voir qui l’a lu' })).toBeTruthy()
    fireEvent.mouseLeave(bouton.parentElement!)
    // Accusé d'une autre conversation : ignoré.
    act(() => diffuserAccuseLecture({ conversationId: 'autre', lecteurId: 'x', luJusquA: new Date().toISOString() }))
  })

  it('direct : envoi « Envoyé » puis « Lu » en direct, sans relire le fil ; coches dans la liste', async () => {
    const s = services()
    const [, direct] = (await s.messagerie.conversations({})).contenu
    page(`/des/messagerie?conversation=${direct.id}`, s)
    const fil = await screen.findByRole('region', { name: 'Aïcha Mahamat Nour' })
    expect(await within(fil).findByRole('img', { name: 'Lu' })).toBeTruthy()
    fireEvent.change(within(fil).getByLabelText('Votre message'), { target: { value: 'Je signe ce soir.' } })
    fireEvent.click(within(fil).getByRole('button', { name: 'Envoyer' }))
    expect(await within(fil).findByRole('img', { name: 'Envoyé' })).toBeTruthy()
    const liste = screen.getByRole('list', { name: 'Conversations' })
    await waitFor(() => expect(within(liste).getByRole('img', { name: 'Envoyé' })).toBeTruthy())
    const messages = vi.spyOn(s.messagerie, 'messages')
    act(() => {
      s.messagerie.lireDemo({ conversationId: direct.id })
    })
    await waitFor(() => expect(within(fil).queryByRole('img', { name: 'Envoyé' })).toBeNull())
    expect(within(fil).getAllByRole('img', { name: 'Lu' })).toHaveLength(2)
    expect(within(liste).getByRole('img', { name: 'Lu' })).toBeTruthy()
    expect(messages).not.toHaveBeenCalled()
  })
})
