import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { ApiError, NotificationsProvider, notificationsSimulees } from '@gimac/socle'
import { PreferencesNotificationsSection } from './Notifications'
import MonComptePage from '../MonComptePage'
import { rendre } from '../../test/rendu'

// Préférences des notifications de la plateforme dans « Mon compte » (D-80, docs/lot1/notifications.md §4) :
// types, notification du système (permission demandée par un bouton, HTTPS exigé), son ; PUT /preferences.

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
const secure = (v: boolean) => Object.defineProperty(window, 'isSecureContext', { value: v, configurable: true })

describe('Mon compte : notifications', () => {
  it('QUO-08 : seuls les types qui peuvent concerner le compte (collaborateur sans équipe ni homologation)', async () => {
    const s = notificationsSimulees({ latenceMs: 0 })
    const session = { sujet: 'u-collab', nom: 'Nadège Mvondo Assiga', identifiant: 'n.mvondo', roles: ['ROLE_COLLABORATEUR'], permissions: ['COL_MESSAGE_READ', 'COL_MESSAGE_SEND', 'PIL_ACTIVITY_OWN', 'NOT_INBOX_READ'], institutionId: '', acr: '2', doubleFacteur: true }
    rendre(<NotificationsProvider service={s} couche="des" actif><PreferencesNotificationsSection /></NotificationsProvider>, { session })
    expect(await screen.findByRole('switch', { name: 'Tâches qui me sont confiées' })).toBeTruthy()
    expect(screen.getByRole('switch', { name: 'Processus d’unité : étapes à exécuter, à signer ou à valider' })).toBeTruthy()
    expect(screen.queryByRole('switch', { name: 'Rapports qui me sont soumis' })).toBeNull()
    expect(screen.queryByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' })).toBeNull()
    expect(screen.queryByRole('switch', { name: 'Demandes à l’administration : soumises ou décidées' })).toBeNull()
  })

  it('absente sans notifications dans la couche (portail)', async () => {
    rendre(<MonComptePage />)
    await waitFor(() => expect(screen.getAllByRole('heading').length).toBeGreaterThan(3))
    expect(screen.queryByRole('region', { name: 'Notifications' })).toBeNull()
  })

  it('bascules enregistrées aussitôt, champ changé seulement ; permission du navigateur demandée par le bouton', async () => {
    secure(true)
    const N = { permission: 'default', requestPermission: vi.fn(async () => { N.permission = 'granted' }) }
    vi.stubGlobal('Notification', N)
    const s = notificationsSimulees({ latenceMs: 0 })
    const put = vi.spyOn(s, 'enregistrerPreferences')
    rendre(<NotificationsProvider service={s} couche="des" actif><PreferencesNotificationsSection /></NotificationsProvider>)
    const tache = await screen.findByRole('switch', { name: 'Tâches qui me sont confiées' })
    expect((tache as HTMLInputElement).checked).toBe(true)
    fireEvent.click(tache)
    await waitFor(() => expect(put).toHaveBeenCalledWith({ types: { MESSAGE: true, TACHE: false, RAPPORT: true, MENTION: true, REPONSE: true, DEMANDE: true, PROCESSUS: true, HOMOLOGATION: true } }))
    expect(await screen.findByText('Préférences de notification enregistrées.')).toBeTruthy()
    await waitFor(() => expect((screen.getByRole('switch', { name: 'Tâches qui me sont confiées' }) as HTMLInputElement).checked).toBe(false))
    // Mentions et réponses (D-82) : interrupteurs propres.
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Mentions : quand on me mentionne avec @' }).hasAttribute('disabled')).toBe(false))
    fireEvent.click(screen.getByRole('switch', { name: 'Mentions : quand on me mentionne avec @' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith({ types: expect.objectContaining({ MENTION: false, REPONSE: true }) }))
    // Demandes à l'administration (D-81) et processus d'unité (D-87) : types.DEMANDE et types.PROCESSUS.
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Processus d’unité : étapes à exécuter, à signer ou à valider' }).hasAttribute('disabled')).toBe(false))
    expect(screen.getByRole('switch', { name: 'Demandes à l’administration : soumises ou décidées' })).toBeTruthy()
    fireEvent.click(screen.getByRole('switch', { name: 'Processus d’unité : étapes à exécuter, à signer ou à valider' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith({ types: expect.objectContaining({ PROCESSUS: false, DEMANDE: true }) }))
    // Homologation (§11) : types.HOMOLOGATION.
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' }).hasAttribute('disabled')).toBe(false))
    fireEvent.click(screen.getByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith({ types: expect.objectContaining({ HOMOLOGATION: false, PROCESSUS: false }) }))
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Son discret à l’arrivée d’une notification' }).hasAttribute('disabled')).toBe(false))
    fireEvent.click(screen.getByRole('switch', { name: 'Son discret à l’arrivée d’une notification' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith({ son: true }))
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Notification du système quand l’onglet est en arrière-plan' }).hasAttribute('disabled')).toBe(false))
    fireEvent.click(screen.getByRole('switch', { name: 'Notification du système quand l’onglet est en arrière-plan' }))
    await waitFor(() => expect(put).toHaveBeenLastCalledWith({ systeme: false }))
    // Jamais demandée à l'ouverture : seulement par le bouton.
    expect(N.requestPermission).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Autoriser dans le navigateur' }))
    expect(await screen.findByText('Autorisées par le navigateur')).toBeTruthy()
    expect(N.requestPermission).toHaveBeenCalledTimes(1)
  })

  it('hors contexte sécurisé : option indisponible, HTTPS exigé ; refus et erreurs', async () => {
    secure(false)
    const s = notificationsSimulees({ latenceMs: 0 })
    s.enregistrerPreferences = () => Promise.reject(new ApiError(400, 'NOT_REQUETE_INVALIDE', 'Type de notification inconnu.', 'cid-1'))
    rendre(<NotificationsProvider service={s} couche="console" actif><PreferencesNotificationsSection /></NotificationsProvider>)
    expect(await screen.findByText('Notification du système indisponible : la plateforme doit être ouverte en HTTPS.')).toBeTruthy()
    const systeme = screen.getByRole('switch', { name: 'Notification du système quand l’onglet est en arrière-plan' }) as HTMLInputElement
    expect(systeme.disabled).toBe(true)
    expect(systeme.checked).toBe(false)
    fireEvent.click(screen.getByRole('switch', { name: 'Nouveaux messages de la messagerie' }))
    expect(await screen.findByText('Type de notification inconnu.')).toBeTruthy()
    cleanup()
    secure(true)
    vi.stubGlobal('Notification', { permission: 'denied', requestPermission: vi.fn() })
    rendre(<NotificationsProvider service={notificationsSimulees({ latenceMs: 0 })} couche="des" actif><PreferencesNotificationsSection /></NotificationsProvider>)
    expect(await screen.findByText('Notifications refusées par le navigateur : autorisez-les dans les réglages du site.')).toBeTruthy()
    cleanup()
    vi.stubGlobal('Notification', undefined)
    const ko = notificationsSimulees({ latenceMs: 0 })
    rendre(<NotificationsProvider service={ko} couche="des" actif><PreferencesNotificationsSection /></NotificationsProvider>)
    expect(await screen.findByText('Ce navigateur ne prend pas en charge les notifications du système.')).toBeTruthy()
    cleanup()
    const panne = notificationsSimulees({ latenceMs: 0 })
    panne.preferences = () => Promise.reject(new Error('panne'))
    rendre(<NotificationsProvider service={panne} couche="des" actif><PreferencesNotificationsSection /></NotificationsProvider>)
    expect(await screen.findByText('Les préférences de notification n’ont pas pu être lues.')).toBeTruthy()
  })
})
