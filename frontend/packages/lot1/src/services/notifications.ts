import type { QueryClient } from '@tanstack/react-query'
import { notificationsSimulees, referenceConversation, type ArriveeNotification, type LecteurApercu, type ServiceNotificationsSimule } from '@gimac/socle'
import type { OptionsReceptionDemo, ReceptionDemo } from './simule/messagerie'
import type { ServicesSimules } from './simule'
import type { Services } from './ports'

// Notifications de la plateforme (D-80, docs/lot1/notifications.md) côté lot 1 : l'aperçu d'un message est lu
// auprès de la messagerie (MS-COLLABORATION, membres seulement : dernier message de la conversation), jamais
// auprès de MS-NOTIFICATION, qui ne porte aucun contenu. En démonstration, les messages reçus de la messagerie
// simulée créent les notifications comme le ferait MS-NOTIFICATION à partir de collaboration.message-sent.v1.

/**
 * Aperçu d'une notification de la messagerie : extrait du dernier message de la conversation référencée ; pour
 * une mention ou une réponse (D-82), extrait du message lui-même, cherché dans la dernière page du fil.
 */
export function lecteurApercu(services: Pick<Services, 'messagerie'>): LecteurApercu {
  return async (n) => {
    if ((n.type === 'MENTION' || n.type === 'REPONSE') && n.conversationId && n.reference.type === 'MESSAGE') {
      const fil = await services.messagerie.messages(n.conversationId)
      const m = fil.contenu.find((x) => x.id === n.reference.id)
      return m?.contenu ? m.contenu.slice(0, 120) : null
    }
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
    conversationId: c.id,
    acteur: { id: r.message.auteur.id, nomAffiche: r.message.auteur.nom },
    le: r.message.envoyeLe,
  }
}

/**
 * Mention ou réponse déduite d'un message reçu (docs/lot1/notifications.md §7) : MENTION si l'utilisateur est
 * mentionné (même s'il est aussi l'auteur cité), sinon REPONSE s'il est l'auteur du message cité ; null sinon.
 */
export function arriveeMention(r: ReceptionDemo, moi: string): ArriveeNotification | null {
  const m = r.message
  const mentionne = (m.mentions ?? []).some((x) => x.id === moi)
  const cite = m.reponseA?.auteur?.id === moi
  if (m.moi || (!mentionne && !cite)) return null
  const c = r.conversation
  return {
    type: mentionne ? 'MENTION' : 'REPONSE',
    titre: mentionne ? `${m.auteur.nom} vous a mentionné` : `${m.auteur.nom} a répondu à votre message`,
    sousTitre: c.type === 'GROUPE' ? c.titre ?? c.liaison?.reference ?? null : null,
    lien: `/des/messagerie?conversation=${encodeURIComponent(c.id)}&message=${encodeURIComponent(m.id)}`,
    reference: { service: 'collaboration', type: 'MESSAGE', id: m.id },
    conversationId: c.id,
    acteur: { id: m.auteur.id, nomAffiche: m.auteur.nom },
    le: m.envoyeLe,
  }
}

/** Accès de démonstration (tests de bout en bout) : un autre membre écrit dans une conversation. */
export type DemoNotifications = { recevoirMessage(o?: OptionsReceptionDemo): string }

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
    const mention = arriveeMention(r, o.moi ?? 'U-001')
    if (mention) notifications.recevoir(mention)
    void o.qc?.invalidateQueries({ queryKey: ['collaboration'] })
  })
  const demo: DemoNotifications = { recevoirMessage: (x) => services.messagerie.recevoirDemo(x).conversation.id }
  ;(window as unknown as { gmDemoNotifications?: DemoNotifications }).gmDemoNotifications = demo
  return notifications
}
