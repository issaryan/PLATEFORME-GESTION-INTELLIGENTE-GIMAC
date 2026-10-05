import type { QueryClient } from '@tanstack/react-query'
import { notificationsSimulees, referenceConversation, type ArriveeNotification, type LecteurApercu, type ServiceNotificationsSimule } from '@gimac/socle'
import type { ReceptionDemo } from './simule/messagerie'
import type { ServicesSimules } from './simule'
import type { Services } from './ports'

// Notifications de la plateforme (D-80, docs/lot1/notifications.md) côté lot 1 : l'aperçu d'un message est lu
// auprès de la messagerie (MS-COLLABORATION, membres seulement : dernier message de la conversation), jamais
// auprès de MS-NOTIFICATION, qui ne porte aucun contenu. En démonstration, les messages reçus de la messagerie
// simulée créent les notifications comme le ferait MS-NOTIFICATION à partir de collaboration.message-sent.v1.

/** Aperçu d'une notification de message : extrait du dernier message de la conversation référencée. */
export function lecteurApercu(services: Pick<Services, 'messagerie'>): LecteurApercu {
  return async (n) => {
    if (n.type !== 'MESSAGE' || n.reference.type !== 'CONVERSATION') return null
    const c = await services.messagerie.conversation(n.reference.id)
    return c.dernierMessage?.extrait || null
  }
}

/** Notification déduite d'un message reçu : expéditeur en titre, nom du groupe en sous-titre (§2, §6). */
export function arriveeMessage(r: ReceptionDemo): ArriveeNotification {
  const c = r.conversation
  return {
    type: 'MESSAGE',
    titre: r.message.auteur.nom,
    sousTitre: c.type === 'GROUPE' ? c.titre ?? c.liaison?.reference ?? null : null,
    lien: `/des/messagerie?conversation=${encodeURIComponent(c.id)}`,
    reference: referenceConversation(c.id),
    acteur: { id: r.message.auteur.id, nomAffiche: r.message.auteur.nom },
    le: r.message.envoyeLe,
  }
}

/** Accès de démonstration (tests de bout en bout) : un autre membre écrit dans une conversation. */
export type DemoNotifications = { recevoirMessage(o?: { conversationId?: string; contenu?: string }): string }

/**
 * MS-NOTIFICATION simulé, branché sur la messagerie simulée : non-lus de démonstration (une notification
 * regroupée par conversation), une tâche confiée et un rapport reçu ; chaque message reçu crée ou regroupe une
 * notification et rafraîchit la messagerie. `window.gmDemoNotifications` déclenche une réception.
 */
export function notificationsDemo(services: ServicesSimules, o: { qc?: QueryClient; moi?: string } = {}): ServiceNotificationsSimule {
  const il = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString()
  // Jamais de notification pour l'acteur lui-même (§6) : la tâche est confiée par un autre que l'utilisateur.
  const chef = o.moi === 'U-001' ? { id: 'U-020', nomAffiche: 'Bertrand Nguema Obiang' } : { id: 'U-001', nomAffiche: 'Paul Mbarga Atangana' }
  const initiales: ArriveeNotification[] = [
    { type: 'RAPPORT', titre: 'Rapport de Joël Ekotto Mballa', sousTitre: 'Rapport quotidien', lien: '/des/rapports?vue=recus', reference: { service: 'pilotage', type: 'RAPPORT', id: 'RAP-DEMO' },
      acteur: { id: 'U-101', nomAffiche: 'Joël Ekotto Mballa' }, le: il(26 * 60), lueLe: il(20 * 60) },
    { type: 'TACHE', titre: `Tâche confiée par ${chef.nomAffiche}`, sousTitre: null, lien: '/des/taches', reference: { service: 'pilotage', type: 'TACHE', id: 'TCH-DEMO' }, acteur: chef, le: il(90) },
    ...services.messagerie.nonLuesDemo().map((r) => ({ ...arriveeMessage(r), nombre: r.nonLus })),
  ]
  const notifications = notificationsSimulees({ initiales })
  services.messagerie.surReception((r) => {
    notifications.recevoir(arriveeMessage(r))
    void o.qc?.invalidateQueries({ queryKey: ['collaboration'] })
  })
  const demo: DemoNotifications = { recevoirMessage: (x) => services.messagerie.recevoirDemo(x).conversation.id }
  ;(window as unknown as { gmDemoNotifications?: DemoNotifications }).gmDemoNotifications = demo
  return notifications
}
