import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from '../i18n/i18n'
import { configureHttp } from '../http/client'
import { Enveloppe } from '../enveloppe/Enveloppe'
import { notificationsHttp } from './http'
import { notificationsSimulees, type ArriveeNotification, type ServiceNotificationsSimule } from './simule'
import { NotificationsProvider, conversationOuverte, lienDansCouche, useLectureConversation, type LecteurApercu } from './contexte'
import { depuis } from './Cloche'
import { NotificationsPage } from './NotificationsPage'
import { SURGISSANTES_MAX } from './Surgissantes'
import { demanderNotificationSysteme, etatNotificationSysteme, jouerSon, notifierSysteme } from './systeme'
import { referenceConversation } from './modele'

// Notifications de la plateforme (D-80, docs/lot1/notifications.md §4) : service http (chemins et corps),
// service simulé (règles de MS-NOTIFICATION), cloche, panneau, page, fenêtres surgissantes et notification du système.

beforeAll(() => {
  initI18n([], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  Object.defineProperty(document, 'hidden', { value: false, configurable: true })
})

const message = (id: string, auteur: string, o: Partial<ArriveeNotification> = {}): ArriveeNotification => ({
  type: 'MESSAGE', titre: auteur, sousTitre: null, lien: `/des/messagerie?conversation=${id}`, reference: referenceConversation(id), acteur: { id: `u-${auteur}`, nomAffiche: auteur }, ...o,
})
const tache = (id: string, o: Partial<ArriveeNotification> = {}): ArriveeNotification => ({
  type: 'TACHE', titre: 'Tâche confiée par Paul', sousTitre: 'Échéance : 2026-10-09', lien: `/des/taches?tache=${id}`, reference: { service: 'pilotage', type: 'TACHE', id }, acteur: null, ...o,
})

describe('service http des notifications', () => {
  const chemin = (f: { mock: { calls: unknown[][] } }, i: number) => {
    const u = new URL(String(f.mock.calls[i][0]), 'http://localhost')
    return u.pathname + u.search
  }
  const reponse = (status: number, corps?: object) =>
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response(corps ? JSON.stringify(corps) : null, { status, headers: corps ? { 'Content-Type': 'application/json' } : {} }))
  beforeEach(() => configureHttp({ onUnauthorized: async () => false, onForbidden: () => undefined, onCorrelation: () => undefined }))

  it('chemins, requêtes et corps sous /bff/des/notification et /bff/console/notification', async () => {
    const des = notificationsHttp('des')
    const f = reponse(200, { contenu: [], total: 0, page: 0, taille: 20 })
    await des.lister({})
    expect(chemin(f, 0)).toBe('/bff/des/notification/notifications?etat=toutes&page=0&taille=20')
    expect(new Headers(f.mock.calls[0][1]?.headers).get('X-Gimac-Fond')).toBe('1')
    reponse(200, { contenu: [], total: 0, page: 2, taille: 5 })
    await des.lister({ etat: 'non-lues', page: 2, taille: 5 })
    expect(chemin(f, 1)).toBe('/bff/des/notification/notifications?etat=non-lues&page=2&taille=5')
    reponse(200, { nonLues: 4 })
    expect(await des.compteur()).toEqual({ nonLues: 4 })
    expect(chemin(f, 2)).toBe('/bff/des/notification/notifications/compteur')
    reponse(204)
    await des.marquerLue('NOT 1')
    expect(f.mock.calls[3][1]?.method).toBe('POST')
    expect(chemin(f, 3)).toBe('/bff/des/notification/notifications/NOT%201/lecture')
    reponse(204)
    await des.marquerLues()
    expect(chemin(f, 4)).toBe('/bff/des/notification/notifications/lecture')
    expect(f.mock.calls[4][1]?.body).toBeFalsy()
    reponse(204)
    await des.marquerLues({ reference: referenceConversation('c-1') })
    expect(JSON.parse(String(f.mock.calls[5][1]?.body))).toEqual({ reference: { service: 'collaboration', type: 'CONVERSATION', id: 'c-1' } })
    reponse(204)
    await des.marquerLues({})
    expect(f.mock.calls[6][1]?.body).toBeFalsy()

    const console = notificationsHttp('console')
    const prefs = { systeme: true, son: true, types: { MESSAGE: true, TACHE: false, RAPPORT: true } }
    reponse(200, prefs)
    expect(await console.preferences()).toEqual(prefs)
    expect(chemin(f, 7)).toBe('/bff/console/notification/preferences')
    reponse(200, prefs)
    await console.enregistrerPreferences({ son: true })
    expect(f.mock.calls[8][1]?.method).toBe('PUT')
    expect(chemin(f, 8)).toBe('/bff/console/notification/preferences')
    expect(JSON.parse(String(f.mock.calls[8][1]?.body))).toEqual({ son: true })
  })
})

describe('service simulé des notifications', () => {
  it('consolidation par conversation, tri par misAJourLe, compteur, lecture par référence', async () => {
    const s = notificationsSimulees({ latenceMs: 0 })
    s.recevoir(message('c-1', 'Carine', { le: '2026-10-05T08:00:00Z' }))
    s.recevoir(tache('t-1', { le: '2026-10-05T09:00:00Z' }))
    const regroupee = s.recevoir(message('c-1', 'Paul', { le: '2026-10-05T10:00:00Z' }))
    expect(regroupee).toMatchObject({ nombre: 2, titre: 'Paul', misAJourLe: '2026-10-05T10:00:00Z' })
    const page = await s.lister({})
    expect(page.total).toBe(2)
    expect(page.contenu.map((n) => n.type)).toEqual(['MESSAGE', 'TACHE'])
    expect(await s.compteur()).toEqual({ nonLues: 2 })
    await s.marquerLues({ reference: referenceConversation('c-1') })
    expect(await s.compteur()).toEqual({ nonLues: 1 })
    expect((await s.lister({ etat: 'non-lues' })).contenu.map((n) => n.type)).toEqual(['TACHE'])
    // Lue, la conversation reçoit une nouvelle notification au message suivant.
    expect(s.recevoir(message('c-1', 'Paul'))!.nombre).toBe(1)
    await s.marquerLues({ type: 'TACHE' })
    await s.marquerLues()
    expect(await s.compteur()).toEqual({ nonLues: 0 })
  })

  it('marquer lue (404 inconnue), pagination (400), préférences (champ absent gardé, type inconnu 400, type désactivé)', async () => {
    const s = notificationsSimulees({ latenceMs: 0, initiales: [message('c-1', 'Carine'), tache('t-1', { lueLe: '2026-10-01T00:00:00Z' })] })
    const [n] = (await s.lister({ etat: 'non-lues' })).contenu
    await s.marquerLue(n.id)
    await s.marquerLue(n.id)
    await expect(s.marquerLue('inconnue')).rejects.toMatchObject({ status: 404, code: 'NOT_NOTIFICATION_INTROUVABLE' })
    await expect(s.lister({ taille: 0 })).rejects.toMatchObject({ status: 400, code: 'NOT_REQUETE_INVALIDE' })
    await expect(s.lister({ page: -1 })).rejects.toMatchObject({ status: 400 })
    expect((await s.lister({ page: 1, taille: 1 })).contenu).toHaveLength(1)
    expect(await s.preferences()).toEqual({ systeme: true, son: false, types: { MESSAGE: true, TACHE: true, RAPPORT: true, MENTION: true, REPONSE: true, DEMANDE: true, PROCESSUS: true, HOMOLOGATION: true, MONITORING: true, SANDBOX: true } })
    expect(await s.enregistrerPreferences({ son: true, types: { TACHE: false } as never })).toEqual({ systeme: true, son: true, types: { MESSAGE: true, TACHE: false, RAPPORT: true, MENTION: true, REPONSE: true, DEMANDE: true, PROCESSUS: true, HOMOLOGATION: true, MONITORING: true, SANDBOX: true } })
    expect(await s.enregistrerPreferences({ systeme: false })).toMatchObject({ systeme: false, son: true })
    await expect(s.enregistrerPreferences({ types: { AUTRE: true } as never })).rejects.toMatchObject({ status: 400, code: 'NOT_REQUETE_INVALIDE' })
    const rappel = vi.fn()
    const fin = s.ecouter!(rappel)
    expect(s.recevoir(tache('t-2'))).toBeNull()
    expect(rappel).not.toHaveBeenCalled()
    s.recevoir(message('c-2', 'Paul'))
    expect(rappel).toHaveBeenCalledTimes(1)
    fin()
    s.recevoir(message('c-3', 'Paul'))
    expect(rappel).toHaveBeenCalledTimes(1)
  })
})

describe('liens et conversation ouverte', () => {
  it('la console transpose un message et renvoie vers l’application interne pour une tâche', () => {
    expect(lienDansCouche({ type: 'MESSAGE', lien: '/des/messagerie?conversation=c-1' }, 'des')).toBe('/des/messagerie?conversation=c-1')
    expect(lienDansCouche({ type: 'MESSAGE', lien: '/des/messagerie?conversation=c-1' }, 'console')).toBe('/console/messagerie?conversation=c-1')
    expect(lienDansCouche({ type: 'TACHE', lien: '/des/taches?tache=t-1' }, 'console')).toMatch(/^https?:\/\/[^/]+\/des\/taches\?tache=t-1/)
  })
  it('conversation ouverte : messagerie de la couche et même identifiant seulement', () => {
    const n = { type: 'MESSAGE' as const, reference: referenceConversation('c-1') }
    expect(conversationOuverte(n, 'des', { pathname: '/des/messagerie/', search: '?conversation=c-1' })).toBe(true)
    expect(conversationOuverte(n, 'des', { pathname: '/des/messagerie', search: '?conversation=c-2' })).toBe(false)
    expect(conversationOuverte(n, 'console', { pathname: '/des/messagerie', search: '?conversation=c-1' })).toBe(false)
    expect(conversationOuverte({ type: 'TACHE', reference: { service: 'pilotage', type: 'TACHE', id: 'c-1' } }, 'des', { pathname: '/des/messagerie', search: '?conversation=c-1' })).toBe(false)
  })
  it('depuis : à l’instant, minutes, heures, jours', () => {
    const t0 = Date.parse('2026-10-05T12:00:00Z')
    expect(depuis('2026-10-05T11:59:30Z', 'fr', t0)).toBeNull()
    expect(depuis('pas une date', 'fr', t0)).toBeNull()
    expect(depuis('2026-10-05T11:55:00Z', 'fr', t0)).toMatch(/5\smin/)
    expect(depuis('2026-10-05T09:00:00Z', 'fr', t0)).toMatch(/3\sh/)
    expect(depuis('2026-10-03T12:00:00Z', 'fr', t0)).toMatch(/2\sj|avant-hier/)
  })
})

function Ici() {
  const l = useLocation()
  return <span data-testid="ici">{l.pathname + l.search}</span>
}
function Aller() {
  const navigate = useNavigate()
  return <button type="button" onClick={() => void navigate('/des/messagerie?conversation=c-9')}>aller</button>
}
function Lecture({ id }: { id: string }) {
  useLectureConversation(id)
  return null
}

const apercu: LecteurApercu = async (n) => `Dernier message de ${n.titre}`

function monter(s: ServiceNotificationsSimule, o: { chemin?: string; actif?: boolean; contenu?: React.ReactNode; couche?: 'des' | 'console' } = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const couche = o.couche ?? 'des'
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[o.chemin ?? `/${couche}/tableau-de-bord`]}>
        <ThemeProvider theme="light">
          <NotificationsProvider service={s} couche={couche} actif={o.actif ?? true} apercu={apercu}>
            <Enveloppe couche={couche} navigation={[]}>
              <Routes>
                <Route path={`/${couche}/notifications`} element={<NotificationsPage />} />
                <Route path="*" element={<><Ici />{o.contenu}</>} />
              </Routes>
            </Enveloppe>
          </NotificationsProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('cloche de la barre du haut', () => {
  it('sans notifications dans la couche : cloche inerte, sans pastille', () => {
    monter(notificationsSimulees({ latenceMs: 0, initiales: [message('c-1', 'Carine')] }), { actif: false })
    expect(screen.getByRole('button', { name: 'Notifications' }).getAttribute('aria-haspopup')).toBeNull()
    expect(document.querySelector('.gm-topbar-pastille')).toBeNull()
  })

  it('pastille 99+ au-delà de 99 non lues', async () => {
    const initiales = Array.from({ length: 120 }, (_, i) => message(`c-${i}`, `P${i}`))
    monter(notificationsSimulees({ latenceMs: 0, initiales }))
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')?.textContent).toBe('99+'))
    expect(screen.getByRole('button', { name: 'Notifications, 99+ non lues' })).toBeTruthy()
  })

  it('panneau : type, titre, sous-titre, aperçu, nombre ; clic = lien et lue ; tout marquer comme lu ; voir tout', async () => {
    const s = notificationsSimulees({ latenceMs: 0, initiales: [
      message('c-1', 'Carine Ebongue', { sousTitre: 'Groupe Cadrage L-12', nombre: 3, le: '2026-10-05T10:00:00Z' }),
      tache('t-1', { le: '2026-10-05T09:00:00Z' }),
      { type: 'RAPPORT', titre: 'Rapport de Joël', sousTitre: 'Rapport quotidien 2026-10-04', lien: '/des/rapports?vue=recus&rapport=r-1', reference: { service: 'pilotage', type: 'RAPPORT', id: 'r-1' }, acteur: null, le: '2026-10-04T09:00:00Z', lueLe: '2026-10-04T10:00:00Z' },
    ] })
    monter(s)
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')?.textContent).toBe('2'))
    fireEvent.click(screen.getByRole('button', { name: 'Notifications, 2 non lues' }))
    const panneau = screen.getByRole('dialog', { name: 'Centre de notifications' })
    await within(panneau).findByText('Carine Ebongue')
    expect(within(panneau).getByText('Groupe Cadrage L-12')).toBeTruthy()
    expect(await within(panneau).findByText('Dernier message de Carine Ebongue')).toBeTruthy()
    expect(within(panneau).getByText('3 messages')).toBeTruthy()
    expect(within(panneau).getByText('Échéance : 2026-10-09')).toBeTruthy()
    expect(within(panneau).getByRole('button', { name: 'Rapport : Rapport de Joël' })).toBeTruthy()
    expect(within(panneau).getByText('2 non lues')).toBeTruthy()
    fireEvent.click(within(panneau).getByRole('button', { name: 'Tâche : Tâche confiée par Paul, non lue' }))
    expect(screen.getByTestId('ici').textContent).toBe('/des/taches?tache=t-1')
    expect(screen.queryByRole('dialog', { name: 'Centre de notifications' })).toBeNull()
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')?.textContent).toBe('1'))
    fireEvent.click(screen.getByRole('button', { name: 'Notifications, 1 non lue' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Tout marquer comme lu' }))
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')).toBeNull())
    fireEvent.click(screen.getByRole('button', { name: 'Préférences' }))
    expect(screen.getByTestId('ici').textContent).toBe('/des/mon-compte')
    fireEvent.click(screen.getByRole('button', { name: 'Notifications' }))
    fireEvent.click(screen.getByRole('button', { name: 'Voir tout' }))
    expect(await screen.findByRole('heading', { name: 'Notifications' })).toBeTruthy()
  })

  it('panneau vide puis en erreur', async () => {
    const s = notificationsSimulees({ latenceMs: 0 })
    monter(s)
    fireEvent.click(screen.getByRole('button', { name: 'Notifications' }))
    expect(await screen.findByText('Aucune notification.')).toBeTruthy()
    cleanup()
    const ko = notificationsSimulees({ latenceMs: 0 })
    ko.lister = () => Promise.reject(new Error('panne'))
    monter(ko)
    fireEvent.click(screen.getByRole('button', { name: 'Notifications' }))
    expect(await screen.findByText('Les notifications n’ont pas pu être lues.')).toBeTruthy()
  })
})

describe('page des notifications', () => {
  it('non lues par défaut, toutes, pagination par 20, tout marquer comme lu', async () => {
    const initiales = Array.from({ length: 25 }, (_, i) => message(`c-${i}`, `Personne ${String(i).padStart(2, '0')}`, { le: `2026-10-05T10:${String(i).padStart(2, '0')}:00Z` }))
    const s = notificationsSimulees({ latenceMs: 0, initiales: [...initiales, tache('t-1', { lueLe: '2026-10-01T00:00:00Z', le: '2026-09-30T00:00:00Z' })] })
    monter(s, { chemin: '/des/notifications' })
    const liste = await screen.findByRole('list', { name: 'Notifications' })
    expect(within(liste).getAllByRole('button')).toHaveLength(20)
    expect(within(liste).getAllByRole('button')[0].textContent).toContain('Personne 24')
    fireEvent.click(screen.getByRole('button', { name: 'Suivante' }))
    await waitFor(() => expect(within(screen.getByRole('list', { name: 'Notifications' })).getAllByRole('button')).toHaveLength(5))
    fireEvent.click(screen.getByRole('tab', { name: 'Toutes' }))
    await waitFor(() => expect(within(screen.getByRole('list', { name: 'Notifications' })).getAllByRole('button')).toHaveLength(20))
    fireEvent.click(screen.getAllByRole('button', { name: 'Tout marquer comme lu' })[0])
    fireEvent.click(screen.getByRole('tab', { name: 'Non lues' }))
    expect(await screen.findByText('Aucune notification non lue.')).toBeTruthy()
  })

  it('erreur de lecture', async () => {
    const s = notificationsSimulees({ latenceMs: 0 })
    s.lister = () => Promise.reject(new Error('panne'))
    monter(s, { chemin: '/des/notifications' })
    expect(await screen.findByText('Les notifications n’ont pas pu être lues.')).toBeTruthy()
  })
})

describe('fenêtres surgissantes', () => {
  it('QUO-11 : une notification reçue avant la première lecture de la liste surgit aussi', async () => {
    const s = notificationsSimulees({ latenceMs: 200, initiales: [message('c-0', 'Ancienne')] })
    monter(s)
    // Réception immédiate, avant que la liste récente soit lue.
    act(() => void s.recevoir(message('c-9', 'Hervé Kamga Tchoupo')))
    const region = await screen.findByRole('region', { name: 'Notifications reçues' }, { timeout: 3000 })
    expect(await within(region).findByText('Hervé Kamga Tchoupo')).toBeTruthy()
    expect(within(region).queryByText('Ancienne')).toBeNull()
  })

  it('à l’arrivée : titre, aperçu, « Ouvrir » (lien et lue) ; trois au plus ; fermeture', async () => {
    const s = notificationsSimulees({ latenceMs: 0, initiales: [message('c-0', 'Ancienne')] })
    monter(s)
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')?.textContent).toBe('1'))
    // L'état connu au chargement ne surgit pas.
    expect(screen.queryByRole('region', { name: 'Notifications reçues' })).toBeNull()
    act(() => void s.recevoir(message('c-1', 'Carine Ebongue', { sousTitre: 'Groupe Cadrage' })))
    const region = await screen.findByRole('region', { name: 'Notifications reçues' })
    expect(await within(region).findByText('Carine Ebongue')).toBeTruthy()
    expect(within(region).getByText('Groupe Cadrage')).toBeTruthy()
    expect(await within(region).findByText('Dernier message de Carine Ebongue')).toBeTruthy()
    act(() => void s.recevoir(tache('t-1')))
    await within(region).findByText('Tâche confiée par Paul')
    act(() => void s.recevoir(message('c-2', 'Paul')))
    await within(region).findByText('Paul')
    act(() => void s.recevoir(message('c-3', 'Rachel')))
    await within(region).findByText('Rachel')
    expect(region.querySelectorAll('.gm-toast')).toHaveLength(SURGISSANTES_MAX)
    expect(within(region).queryByText('Carine Ebongue')).toBeNull()
    // Regroupement : la notification de la même conversation surgit de nouveau, avec son nombre.
    act(() => void s.recevoir(message('c-3', 'Rachel')))
    await within(region).findByText('Rachel (2)')
    fireEvent.click(within(region).getAllByRole('button', { name: 'Fermer la notification' })[0])
    await waitFor(() => expect(region.querySelectorAll('.gm-toast')).toHaveLength(2))
    const toast = within(region).getByText('Rachel (2)').closest('.gm-toast') as HTMLElement
    fireEvent.click(within(toast).getByRole('button', { name: 'Ouvrir' }))
    expect(screen.getByTestId('ici').textContent).toBe('/des/messagerie?conversation=c-3')
    await waitFor(() => expect(within(screen.getByRole('region', { name: 'Notifications reçues' })).queryByText('Rachel (2)')).toBeNull())
  })

  it('disparaît après 6 s', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    try {
      const s = notificationsSimulees({ latenceMs: 0 })
      monter(s)
      await waitFor(() => expect(screen.getByRole('button', { name: 'Notifications' })).toBeTruthy())
      await act(async () => { await vi.advanceTimersByTimeAsync(50) })
      act(() => void s.recevoir(message('c-1', 'Carine')))
      await screen.findByRole('region', { name: 'Notifications reçues' })
      await act(async () => { await vi.advanceTimersByTimeAsync(6_100) })
      expect(screen.queryByRole('region', { name: 'Notifications reçues' })).toBeNull()
    } finally {
      vi.useRealTimers()
    }
  })

  it('jamais pour la conversation ouverte : marquée lue sans fenêtre ; ouverture d’une conversation : lecture par référence', async () => {
    const s = notificationsSimulees({ latenceMs: 0, initiales: [message('c-9', 'Avant')] })
    const lues = vi.spyOn(s, 'marquerLues')
    monter(s, { chemin: '/des/messagerie?conversation=c-1', contenu: <><Aller /><Lecture id="c-1" /></> })
    await waitFor(() => expect(lues).toHaveBeenCalledWith({ reference: referenceConversation('c-1') }))
    await waitFor(() => expect(document.querySelector('.gm-topbar-pastille')?.textContent).toBe('1'))
    act(() => void s.recevoir(message('c-1', 'Carine')))
    await waitFor(async () => expect(await s.compteur()).toEqual({ nonLues: 1 }))
    expect(screen.queryByRole('region', { name: 'Notifications reçues' })).toBeNull()
    // Une autre conversation surgit.
    act(() => void s.recevoir(message('c-2', 'Paul')))
    expect(await screen.findByRole('region', { name: 'Notifications reçues' })).toBeTruthy()
  })

  it('onglet caché : notification du système (préférence active, permission accordée) et son', async () => {
    const creees: { titre: string; o: NotificationOptions; onclick: (() => void) | null; close: () => void }[] = []
    class FausseNotification {
      static permission = 'granted'
      static requestPermission = vi.fn()
      onclick: (() => void) | null = null
      close = vi.fn()
      constructor(titre: string, o: NotificationOptions) {
        creees.push(Object.assign(this, { titre, o }) as never)
      }
    }
    vi.stubGlobal('Notification', FausseNotification)
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true })
    const focus = vi.spyOn(window, 'focus').mockImplementation(() => undefined)
    const s = notificationsSimulees({ latenceMs: 0, preferences: { son: true } })
    monter(s)
    await waitFor(() => expect(screen.getByRole('button', { name: 'Notifications' })).toBeTruthy())
    await act(async () => { await new Promise((r) => setTimeout(r, 30)) })
    Object.defineProperty(document, 'hidden', { value: true, configurable: true })
    act(() => void s.recevoir(message('c-1', 'Carine')))
    await waitFor(() => expect(creees).toHaveLength(1))
    expect(creees[0].titre).toBe('Carine')
    expect(creees[0].o).toMatchObject({ body: 'Dernier message de Carine', tag: expect.stringMatching(/^NOT-/) })
    expect(screen.queryByRole('region', { name: 'Notifications reçues' })).toBeNull()
    act(() => creees[0].onclick!())
    expect(focus).toHaveBeenCalled()
    expect(creees[0].close).toHaveBeenCalled()
    expect(screen.getByTestId('ici').textContent).toBe('/des/messagerie?conversation=c-1')
    // Tâche : le sous-titre fait le corps.
    act(() => void s.recevoir(tache('t-1')))
    await waitFor(() => expect(creees).toHaveLength(2))
    expect(creees[1].o.body).toBe('Échéance : 2026-10-09')
    // Préférence `systeme` coupée : rien.
    await act(async () => { await s.enregistrerPreferences({ systeme: false }) })
    cleanup()
  })
})

describe('notification du système et son', () => {
  it('hors contexte sécurisé : HTTPS exigé ; API absente ; permission demandée par un geste', async () => {
    Object.defineProperty(window, 'isSecureContext', { value: false, configurable: true })
    expect(etatNotificationSysteme()).toBe('https')
    expect(await demanderNotificationSysteme()).toBe('https')
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true })
    vi.stubGlobal('Notification', undefined)
    expect(etatNotificationSysteme()).toBe('absente')
    const N = { permission: 'default', requestPermission: vi.fn(async () => { N.permission = 'granted' }) }
    vi.stubGlobal('Notification', N)
    expect(etatNotificationSysteme()).toBe('a-demander')
    expect(await demanderNotificationSysteme()).toBe('accordee')
    expect(N.requestPermission).toHaveBeenCalledTimes(1)
    N.permission = 'denied'
    expect(etatNotificationSysteme()).toBe('refusee')
    // Onglet visible ou permission refusée : aucune notification du système.
    expect(notifierSysteme({ titre: 't', etiquette: 'e', ouvrir: () => undefined })).toBe(false)
    Object.defineProperty(document, 'hidden', { value: true, configurable: true })
    expect(notifierSysteme({ titre: 't', etiquette: 'e', ouvrir: () => undefined })).toBe(false)
  })

  it('son discret : muet sans Web Audio ou sous un navigateur piloté ; deux notes sinon', () => {
    vi.stubGlobal('AudioContext', undefined)
    expect(jouerSon()).toBe(false)
    const osc = { type: '', frequency: { value: 0 }, connect: vi.fn(() => ({ connect: vi.fn() })), start: vi.fn(), stop: vi.fn() }
    const gain = { gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() } }
    class FauxContexte {
      currentTime = 0
      destination = {}
      createOscillator = () => osc
      createGain = () => gain
      close = vi.fn(async () => undefined)
    }
    vi.stubGlobal('AudioContext', FauxContexte)
    Object.defineProperty(navigator, 'webdriver', { value: true, configurable: true })
    expect(jouerSon()).toBe(false)
    Object.defineProperty(navigator, 'webdriver', { value: false, configurable: true })
    expect(jouerSon()).toBe(true)
    expect(osc.start).toHaveBeenCalledTimes(2)
    vi.stubGlobal('AudioContext', class { constructor() { throw new Error('refus') } })
    expect(jouerSon()).toBe(false)
  })
})
