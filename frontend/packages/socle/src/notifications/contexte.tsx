import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import type { ApiError } from '../http/errors'
import { adresseApplication } from '../session/aiguillage'
import { lireSessionDemo } from '../session/demo'
import { TYPES_MESSAGERIE, conversationDe, referenceConversation, type EtatNotifications, type FiltreLecture, type NotificationPlateforme, type PreferencesNotifications, type ServiceNotifications } from './modele'

// Notifications de la plateforme (D-80) dans une couche interne : service injecté, hooks de données et
// ouverture d'une notification. Clés de requête sous le sujet « notification » : l'événement
// notification.inapp-created.v1 du canal poussé (et, en services simulés, `ecouter`) les invalide.

export type CoucheNotifications = 'des' | 'console'

/** Aperçu d'un message (dernier message de la conversation référencée), lu auprès de la messagerie. */
export type LecteurApercu = (n: NotificationPlateforme) => Promise<string | null>

type Contexte = { service: ServiceNotifications; couche: CoucheNotifications; apercu?: LecteurApercu }
const Ctx = createContext<Contexte | null>(null)

export const clesNotifications = {
  tout: ['notification'] as const,
  compteur: () => ['notification', 'compteur'] as const,
  liste: (etat: EtatNotifications, page: number, taille: number) => ['notification', 'liste', etat, page, taille] as const,
  // Hors du sujet : ni les préférences ni l'aperçu d'un état donné (misAJourLe) ne changent avec un événement.
  preferences: () => ['notification-preferences'] as const,
  apercu: (id: string, misAJourLe: string) => ['notification-apercu', id, misAJourLe] as const,
}

/** Repli du canal poussé : compteur et notifications récentes relus chaque minute. */
export const SONDAGE_NOTIFICATIONS = 60_000
/** Notifications récentes du panneau de la cloche. */
export const TAILLE_RECENTES = 10

/**
 * Fournit les notifications aux écrans d'une couche interne. `actif` : session portant NOT_INBOX_READ, hors
 * premier accès ; sinon, aucune notification n'est lue et la cloche reste inerte.
 */
export function NotificationsProvider({ service, couche, actif, apercu, children }: {
  service: ServiceNotifications
  couche: CoucheNotifications
  actif: boolean
  apercu?: LecteurApercu
  children: ReactNode
}) {
  const qc = useQueryClient()
  useEffect(() => (actif && service.ecouter ? service.ecouter(() => void qc.invalidateQueries({ queryKey: clesNotifications.tout })) : undefined), [actif, service, qc])
  const valeur = useMemo(() => (actif ? { service, couche, apercu } : null), [actif, service, couche, apercu])
  return <Ctx.Provider value={valeur}>{children}</Ctx.Provider>
}

/** Contexte des notifications, ou null hors couche interne (portail) ou sans la permission. */
export const useContexteNotifications = () => useContext(Ctx)
/** Vrai si la couche sert les notifications à cette session. */
export const useNotificationsDisponibles = () => useContext(Ctx) !== null

function useService() {
  const c = useContext(Ctx)
  if (!c) throw new Error('NotificationsProvider absent')
  return c.service
}

export function useCompteurNotifications() {
  const c = useContext(Ctx)
  return useQuery<{ nonLues: number }, ApiError>({ queryKey: clesNotifications.compteur(), queryFn: () => c!.service.compteur(), enabled: !!c, refetchInterval: SONDAGE_NOTIFICATIONS })
}

export function useListeNotifications(etat: EtatNotifications, page: number, taille: number, sondage = false) {
  const c = useContext(Ctx)
  return useQuery({
    queryKey: clesNotifications.liste(etat, page, taille),
    queryFn: () => c!.service.lister({ etat, page, taille }),
    enabled: !!c,
    placeholderData: keepPreviousData,
    refetchInterval: sondage ? SONDAGE_NOTIFICATIONS : false,
  })
}

/** Notifications récentes (panneau de la cloche, détection des arrivées). */
export const useNotificationsRecentes = () => useListeNotifications('toutes', 0, TAILLE_RECENTES, true)

function useEcriture<V>(agir: (s: ServiceNotifications, v: V) => Promise<unknown>) {
  const s = useService()
  const qc = useQueryClient()
  return useMutation<unknown, ApiError, V>({ mutationFn: (v) => agir(s, v), onSuccess: () => qc.invalidateQueries({ queryKey: clesNotifications.tout }) })
}

export const useMarquerNotificationLue = () => useEcriture((s, id: string) => s.marquerLue(id))
export const useToutMarquerLu = () => useEcriture((s, f: FiltreLecture | undefined) => s.marquerLues(f))

export function usePreferencesNotifications() {
  const c = useContext(Ctx)
  return useQuery<PreferencesNotifications, ApiError>({ queryKey: clesNotifications.preferences(), queryFn: () => c!.service.preferences(), enabled: !!c, staleTime: 60_000 })
}

export function useEnregistrerPreferencesNotifications() {
  const s = useService()
  const qc = useQueryClient()
  const cle = clesNotifications.preferences()
  // Bascule optimiste : la préférence change aussitôt à l'écran, et revient à l'état connu si le service refuse.
  return useMutation<PreferencesNotifications, ApiError, Partial<PreferencesNotifications>, { avant?: PreferencesNotifications }>({
    mutationFn: (p) => s.enregistrerPreferences(p),
    onMutate: (p) => {
      void qc.cancelQueries({ queryKey: cle })
      const avant = qc.getQueryData<PreferencesNotifications>(cle)
      if (avant) qc.setQueryData<PreferencesNotifications>(cle, { ...avant, ...p, types: { ...avant.types, ...p.types } })
      return { avant }
    },
    onError: (_e, _p, ctx) => {
      if (ctx?.avant) qc.setQueryData(cle, ctx.avant)
    },
    onSuccess: (p) => qc.setQueryData(cle, p),
  })
}

/** Aperçu d'un message (message, mention ou réponse, si la couche sait le lire) ; null pour une tâche ou un rapport. */
export function useApercuNotification(n: NotificationPlateforme) {
  const c = useContext(Ctx)
  const lire = c?.apercu
  return useQuery({
    queryKey: clesNotifications.apercu(n.id, n.misAJourLe),
    queryFn: () => lire!(n),
    enabled: TYPES_MESSAGERIE.includes(n.type) && !!lire,
    staleTime: Infinity,
    retry: false,
  })
}

/**
 * Lien d'une notification dans une couche. MS-NOTIFICATION écrit des liens de l'application interne (/des/…) :
 * la console y substitue son préfixe pour un message (la messagerie y est servie) et renvoie vers l'adresse
 * de l'application interne pour une tâche ou un rapport, qui n'existent que là.
 */
export function lienDansCouche(n: Pick<NotificationPlateforme, 'type' | 'lien'>, couche: CoucheNotifications): string {
  if (couche === 'des') return n.lien
  if (TYPES_MESSAGERIE.includes(n.type) && /^\/des(\/|$|\?)/.test(n.lien)) return `/console${n.lien.slice(4)}`
  return adresseApplication('des', n.lien, lireSessionDemo())
}

/**
 * Vrai si la notification porte sur la conversation actuellement ouverte dans la messagerie de la couche
 * (/{couche}/messagerie?conversation=<id>) : pas de fenêtre surgissante pour elle.
 */
export function conversationOuverte(n: Pick<NotificationPlateforme, 'type' | 'reference' | 'conversationId'>, couche: CoucheNotifications, emplacement: { pathname: string; search: string }): boolean {
  // Message, mention ou réponse (D-82) : comparaison sur la conversation de la notification.
  const conversation = conversationDe(n)
  if (!conversation) return false
  if (emplacement.pathname.replace(/\/+$/, '') !== `/${couche}/messagerie`) return false
  return new URLSearchParams(emplacement.search).get('conversation') === conversation
}

/** Ouvre une notification : marquée lue si elle ne l'est pas, puis son lien (routeur, ou adresse d'une autre application). */
export function useOuvrirNotification() {
  const c = useContext(Ctx)
  const navigate = useNavigate()
  const lue = useMarquerNotificationLue()
  return useCallback((n: NotificationPlateforme) => {
    if (!c) return
    if (!n.lueLe) lue.mutate(n.id)
    const href = lienDansCouche(n, c.couche)
    if (href.startsWith('/')) void navigate(href)
    else window.location.assign(href)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [c, navigate])
}

/**
 * Ouverture d'une conversation de la messagerie : ses notifications sont marquées lues
 * (POST /notifications/lecture avec la référence de la conversation), à l'ouverture et à chaque nouveau
 * message reçu pendant qu'elle est ouverte (`dernier`). Sans notifications dans la couche : rien.
 */
export function useLectureConversation(conversationId: string | undefined, dernier?: string) {
  const c = useContext(Ctx)
  const qc = useQueryClient()
  useEffect(() => {
    if (!c || !conversationId) return
    c.service.marquerLues({ reference: referenceConversation(conversationId) })
      .then(() => qc.invalidateQueries({ queryKey: clesNotifications.tout }))
      .catch(() => undefined)
  }, [c, qc, conversationId, dernier])
}
