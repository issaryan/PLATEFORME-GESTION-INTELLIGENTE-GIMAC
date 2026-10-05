import { http } from '../http/client'
import { P } from '../http/prefixes'
import type { PreferencesNotifications, ServiceNotifications } from './modele'

// Implémentation HTTP des notifications (D-80, docs/lot1/notifications.md §3) : MS-NOTIFICATION derrière la
// passerelle, /bff/{des,console}/notification réécrit vers /api/v1. Liste et compteur sont relus par le canal
// poussé et le sondage de repli : appels de fond, qui ne prolongent pas la session d'inactivité (D-65 §6).

export function notificationsHttp(couche: 'des' | 'console'): ServiceNotifications {
  const N = couche === 'console' ? P.consoleNotification : P.desNotification
  return {
    lister: (q) => http.get(`${N}/notifications`, { fond: true, query: { etat: q.etat ?? 'toutes', page: q.page ?? 0, taille: q.taille ?? 20 } }),
    compteur: () => http.get(`${N}/notifications/compteur`, { fond: true }),
    marquerLue: (id) => http.post(`${N}/notifications/${encodeURIComponent(id)}/lecture`),
    // Corps facultatif : sans filtre, toutes mes notifications ; sinon celles d'un type ou d'un objet.
    marquerLues: (f) => http.post(`${N}/notifications/lecture`, f && (f.type || f.reference) ? { body: f } : {}),
    preferences: () => http.get(`${N}/preferences`),
    enregistrerPreferences: (p: Partial<PreferencesNotifications>) => http.put(`${N}/preferences`, { body: p }),
  }
}
