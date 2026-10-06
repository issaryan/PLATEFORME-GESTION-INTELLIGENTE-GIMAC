import { ApiError } from '../http/errors'
import type { FiltreLecture, NotificationPlateforme, PreferencesNotifications, ServiceNotifications, TypeNotification } from './modele'
import { TYPES_NOTIFICATION, conversationDe } from './modele'

// Notifications simulées (D-80) : mêmes formes et mêmes codes que MS-NOTIFICATION (docs/lot1/notifications.md
// §3, §6). Imitation des règles du service : consolidation par objet (une seule notification non lue par
// conversation, `nombre` incrémenté et remontée en tête), type désactivé dans les préférences sans création,
// tri par misAJourLe décroissant, 404 NOT_NOTIFICATION_INTROUVABLE. `recevoir` tient lieu de l'événement
// consommé (message, tâche, rapport) : les abonnés (`ecouter`) sont prévenus comme par le canal poussé.

/** Arrivée simulée : ce que MS-NOTIFICATION déduirait de l'événement consommé. */
export type ArriveeNotification = Omit<NotificationPlateforme, 'id' | 'nombre' | 'creeLe' | 'misAJourLe' | 'lueLe'> & { le?: string; nombre?: number; lueLe?: string | null }

export type ServiceNotificationsSimule = ServiceNotifications & {
  /** Dépose une notification (ou regroupe dans la non lue du même objet) ; null si le type est désactivé. */
  recevoir(a: ArriveeNotification): NotificationPlateforme | null
}

export type OptionsNotificationsSimulees = {
  latenceMs?: number
  initiales?: ArriveeNotification[]
  preferences?: Partial<PreferencesNotifications>
}

const PREFERENCES_DEFAUT: PreferencesNotifications = { systeme: true, son: false, types: { MESSAGE: true, TACHE: true, RAPPORT: true, MENTION: true, REPONSE: true, DEMANDE: true, PROCESSUS: true } }

export function notificationsSimulees(o: OptionsNotificationsSimulees = {}): ServiceNotificationsSimule {
  const latence = o.latenceMs ?? 150
  let sequence = 0
  const notifications: NotificationPlateforme[] = []
  let preferences: PreferencesNotifications = { ...PREFERENCES_DEFAUT, ...o.preferences, types: { ...PREFERENCES_DEFAUT.types, ...o.preferences?.types } }
  const abonnes = new Set<() => void>()
  const refus = (status: number, code: string, message: string) => new ApiError(status, code, message, `sim-not-${Date.now().toString(36)}`)
  const attendre = <T>(f: () => T): Promise<T> =>
    new Promise((ok, ko) =>
      window.setTimeout(() => {
        try {
          ok(structuredClone(f()))
        } catch (e) {
          ko(e)
        }
      }, latence),
    )
  const prevenir = () => abonnes.forEach((a) => a())
  const triees = () => [...notifications].sort((a, b) => b.misAJourLe.localeCompare(a.misAJourLe))
  const memeObjet = (a: NotificationPlateforme, b: Pick<NotificationPlateforme, 'reference'>) =>
    a.reference.service === b.reference.service && a.reference.type === b.reference.type && a.reference.id === b.reference.id

  const deposer = (a: ArriveeNotification): NotificationPlateforme | null => {
    if (!preferences.types[a.type]) return null
    const le = a.le ?? new Date().toISOString()
    const { le: _le, nombre, lueLe, ...reste } = a
    // Consolidation : la notification non lue du même objet est incrémentée et remonte en tête.
    const existante = a.type === 'MESSAGE' && !lueLe ? notifications.find((n) => !n.lueLe && memeObjet(n, a)) : undefined
    if (existante) {
      Object.assign(existante, { titre: a.titre, sousTitre: a.sousTitre, acteur: a.acteur, nombre: existante.nombre + (nombre ?? 1), misAJourLe: le })
      return existante
    }
    const n: NotificationPlateforme = { conversationId: null, ...reste, id: `NOT-${(++sequence).toString().padStart(4, '0')}`, nombre: nombre ?? 1, creeLe: le, misAJourLe: le, lueLe: lueLe ?? null }
    notifications.push(n)
    return n
  }
  ;(o.initiales ?? []).forEach(deposer)

  // Référence d'une conversation : ses messages, et ses mentions et réponses (D-82, conversationId).
  const vise = (n: NotificationPlateforme, r: NotificationPlateforme['reference']) =>
    memeObjet(n, { reference: r }) || (r.service === 'collaboration' && r.type === 'CONVERSATION' && conversationDe(n) === r.id)
  const lire = (f: FiltreLecture = {}) => {
    const le = new Date().toISOString()
    notifications
      .filter((n) => !n.lueLe && (!f.type || n.type === f.type) && (!f.reference || vise(n, f.reference)))
      .forEach((n) => (n.lueLe = le))
  }

  return {
    lister: (q) => attendre(() => {
      const taille = q.taille ?? 20
      const page = q.page ?? 0
      if (taille < 1 || taille > 100 || page < 0) throw refus(400, 'NOT_REQUETE_INVALIDE', 'Pagination invalide.')
      const l = triees().filter((n) => q.etat !== 'non-lues' || !n.lueLe)
      return { contenu: l.slice(page * taille, (page + 1) * taille), total: l.length, page, taille }
    }),
    compteur: () => attendre(() => ({ nonLues: notifications.filter((n) => !n.lueLe).length })),
    marquerLue: (id) => attendre(() => {
      const n = notifications.find((x) => x.id === id)
      if (!n) throw refus(404, 'NOT_NOTIFICATION_INTROUVABLE', 'Notification introuvable.')
      n.lueLe ??= new Date().toISOString()
    }),
    marquerLues: (f) => attendre(() => lire(f)),
    preferences: () => attendre(() => preferences),
    enregistrerPreferences: (p) => attendre(() => {
      if (p.types && Object.keys(p.types).some((t) => !TYPES_NOTIFICATION.includes(t as TypeNotification))) throw refus(400, 'NOT_REQUETE_INVALIDE', 'Type de notification inconnu.')
      // Un champ absent garde sa valeur (§6).
      preferences = { systeme: p.systeme ?? preferences.systeme, son: p.son ?? preferences.son, types: { ...preferences.types, ...p.types } }
      return preferences
    }),
    ecouter: (rappel) => {
      abonnes.add(rappel)
      return () => abonnes.delete(rappel)
    },
    recevoir: (a) => {
      const n = deposer(a)
      if (n) prevenir()
      return n ? structuredClone(n) : null
    },
  }
}
