import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from '../i18n/i18n'
import { configureHttp, http } from '../http/client'
import { Enveloppe } from '../enveloppe/Enveloppe'
import { useBarreLaterale } from '../enveloppe/barreLaterale'
import { notificationsSimulees, type ArriveeNotification } from './simule'
import { NotificationsProvider, conversationOuverte, lienDansCouche } from './contexte'
import { conversationDe, referenceConversation, TYPES_NOTIFICATION } from './modele'

// Mentions et réponses (D-82, docs/lot1/notifications.md §7), corps binaire du client (aperçu bureautique, D-83),
// barre latérale réduite par un écran (D-84).

beforeAll(() => {
  initI18n([], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const mention = (cid: string, mid: string, type: 'MENTION' | 'REPONSE' = 'MENTION'): ArriveeNotification => ({
  type, titre: type === 'MENTION' ? 'Hervé vous a mentionné' : 'Hervé a répondu à votre message', sousTitre: null,
  lien: `/des/messagerie?conversation=${cid}&message=${mid}`, reference: { service: 'collaboration', type: 'MESSAGE', id: mid }, conversationId: cid,
  acteur: { id: 'u-rssi', nomAffiche: 'Hervé' },
})

describe('notifications de mention et de réponse', () => {
  it('types, conversation d’une notification, conversation ouverte, lien de la console', () => {
    expect(TYPES_NOTIFICATION).toEqual(['MESSAGE', 'TACHE', 'RAPPORT', 'MENTION', 'REPONSE', 'DEMANDE', 'PROCESSUS', 'HOMOLOGATION', 'MONITORING', 'SANDBOX'])
    const n = { type: 'MENTION' as const, reference: { service: 'collaboration', type: 'MESSAGE', id: 'm-1' }, conversationId: 'c-1' }
    expect(conversationDe(n)).toBe('c-1')
    expect(conversationDe({ type: 'MESSAGE', reference: referenceConversation('c-2') })).toBe('c-2')
    expect(conversationDe({ type: 'REPONSE', reference: { service: 'collaboration', type: 'MESSAGE', id: 'm' } })).toBeNull()
    expect(conversationDe({ type: 'TACHE', reference: { service: 'pilotage', type: 'TACHE', id: 't' }, conversationId: 'c-1' })).toBeNull()
    expect(conversationOuverte(n, 'des', { pathname: '/des/messagerie', search: '?conversation=c-1&message=m-1' })).toBe(true)
    expect(conversationOuverte(n, 'des', { pathname: '/des/messagerie', search: '?conversation=c-9' })).toBe(false)
    expect(lienDansCouche({ type: 'REPONSE', lien: '/des/messagerie?conversation=c-1&message=m-1' }, 'console')).toBe('/console/messagerie?conversation=c-1&message=m-1')
    // Demandes (D-81) et processus d'unité (D-87) : lien rendu tel quel dans la couche interne, adresse de l'application interne depuis la console.
    expect(lienDansCouche({ type: 'PROCESSUS', lien: '/des/processus/instances/i-1?etape=ANALYSE' }, 'des')).toBe('/des/processus/instances/i-1?etape=ANALYSE')
    expect(lienDansCouche({ type: 'PROCESSUS', lien: '/des/processus/instances/i-1?etape=ANALYSE' }, 'console')).toMatch(/^https?:\/\/[^/]+\/des\/processus\/instances\/i-1\?etape=ANALYSE/)
    expect(lienDansCouche({ type: 'DEMANDE', lien: '/des/administration/demandes?demande=DEM-1' }, 'console')).toMatch(/\/des\/administration\/demandes\?demande=DEM-1/)
  })

  it('service simulé : jamais regroupées, préférences propres, lues avec la conversation', async () => {
    vi.useFakeTimers()
    try {
      const s = notificationsSimulees({ latenceMs: 0 })
      const lire = async <T,>(p: Promise<T>) => { await vi.runAllTimersAsync(); return p }
      expect((await lire(s.preferences())).types).toMatchObject({ MENTION: true, REPONSE: true })
      expect(s.recevoir(mention('c-1', 'm-1'))!.conversationId).toBe('c-1')
      s.recevoir(mention('c-1', 'm-2', 'REPONSE'))
      s.recevoir(mention('c-1', 'm-2', 'REPONSE'))
      expect((await lire(s.compteur())).nonLues).toBe(3)
      await lire(s.marquerLues({ reference: referenceConversation('c-1') }))
      expect((await lire(s.compteur())).nonLues).toBe(0)
      await lire(s.enregistrerPreferences({ types: { MENTION: false } as never }))
      expect(s.recevoir(mention('c-1', 'm-3'))).toBeNull()
      const sansConversation = s.recevoir({ type: 'TACHE', titre: 'T', sousTitre: null, lien: '/des/taches', reference: { service: 'pilotage', type: 'TACHE', id: 't' }, acteur: null })
      expect(sansConversation!.conversationId).toBeNull()
    } finally {
      vi.useRealTimers()
    }
  })

  it('cloche : pictogrammes « @ » et « répondre », libellés des types', async () => {
    const s = notificationsSimulees({ latenceMs: 0, initiales: [mention('c-1', 'm-1'), mention('c-2', 'm-2', 'REPONSE')] })
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <MemoryRouter initialEntries={['/des/tableau-de-bord']}>
          <ThemeProvider theme="light">
            <NotificationsProvider service={s} couche="des" actif apercu={async () => 'Pouvez-vous valider ?'}>
              <Enveloppe couche="des" navigation={[]}><Routes><Route path="*" element={<span />} /></Routes></Enveloppe>
            </NotificationsProvider>
          </ThemeProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    )
    // Sous charge (suites lancées ensemble), le premier rendu de l'enveloppe dépasse la seconde d'attente par défaut.
    const attente = { timeout: 10_000 }
    fireEvent.click(await screen.findByRole('button', { name: 'Notifications, 2 non lues' }, attente))
    expect(await screen.findByRole('button', { name: 'Mention : Hervé vous a mentionné, non lue' }, attente)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Réponse : Hervé a répondu à votre message, non lue' })).toBeTruthy()
    await waitFor(() => expect(screen.getAllByText('Pouvez-vous valider ?').length).toBe(2), attente)
  })
})

describe('client http : corps binaire, en-têtes propres, réponse binaire (D-83)', () => {
  it('Blob envoyé tel quel avec son type ou celui donné, réponse lue en Blob', async () => {
    configureHttp({ csrf: null })
    const f = vi.spyOn(globalThis, 'fetch').mockImplementation(async () => new Response('%PDF', { status: 200, headers: { 'Content-Type': 'application/pdf' } }))
    const source = new Blob(['PK'], { type: 'application/vnd.ms-powerpoint' })
    const r = await http.post<Blob>('/bff/des/document/apercus', { body: source, binaire: true, accept: 'application/pdf, application/problem+json', entetes: { 'X-Nom-Fichier': 'plan%20v2.ppt' } })
    expect(await r.text()).toBe('%PDF')
    const h = new Headers(f.mock.calls[0][1]?.headers)
    expect(h.get('Content-Type')).toBe('application/vnd.ms-powerpoint')
    expect(h.get('X-Nom-Fichier')).toBe('plan%20v2.ppt')
    expect(f.mock.calls[0][1]?.body).toBe(source)
    await http.post('/x', { body: new Blob(['a']), contentType: 'application/msword', binaire: true })
    expect(new Headers(f.mock.calls[1][1]?.headers).get('Content-Type')).toBe('application/msword')
    await http.post('/x', { body: new Blob(['a']), binaire: true })
    expect(new Headers(f.mock.calls[2][1]?.headers).get('Content-Type')).toBe('application/octet-stream')
  })
  it('413 de la passerelle sans corps JSON : statut et code HTTP_413', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 413 }))
    await expect(http.post('/bff/des/document/apercus', { body: new Blob(['x']), binaire: true })).rejects.toMatchObject({ status: 413, code: 'HTTP_413' })
  })
})

function Ecran() {
  const b = useBarreLaterale()
  return (
    <span>
      <span data-testid="etat">{`${b.active}-${b.reduite}`}</span>
      <button type="button" onClick={() => b.reduire(!b.reduite)}>basculer</button>
    </span>
  )
}

describe('barre latérale réduite par un écran (D-84)', () => {
  it('mode icônes demandé par l’écran, puis rétabli ; hors enveloppe, inactive', () => {
    render(
      <MemoryRouter initialEntries={['/des/messagerie']}>
        <ThemeProvider theme="light">
          <Enveloppe couche="des" navigation={[{ entrees: [{ cle: 'msg', libelleKey: 'x.msg', icone: 'messages-square', chemin: '/des/messagerie' }] }]}>
            <Ecran />
          </Enveloppe>
        </ThemeProvider>
      </MemoryRouter>,
    )
    const nav = () => document.querySelector('.gm-shell-nav .gm-sidenav')!
    expect(screen.getByTestId('etat').textContent).toBe('true-false')
    expect(nav().classList.contains('gm-sidenav-reduite')).toBe(false)
    act(() => fireEvent.click(screen.getByRole('button', { name: 'basculer' })))
    expect(nav().classList.contains('gm-sidenav-reduite')).toBe(true)
    act(() => fireEvent.click(screen.getByRole('button', { name: 'basculer' })))
    expect(nav().classList.contains('gm-sidenav-reduite')).toBe(false)
    cleanup()
    render(<Ecran />)
    expect(screen.getByTestId('etat').textContent).toBe('false-false')
    fireEvent.click(screen.getByRole('button', { name: 'basculer' }))
  })
})
