import type { QueryClient } from '@tanstack/react-query'
import { notificationsSimulees, referenceConversation, type ArriveeNotification, type LecteurApercu, type ServiceNotificationsSimule } from '@gimac/socle'
import type { OptionsLectureDemo, OptionsReceptionDemo, ReceptionDemo } from './simule/messagerie'
import type { DecisionDemo, DecisionTrancheeDemo, ServicesSimules } from './simule'
import type { NotificationOperationDemo } from './simule/parcours'
import { ORGANISATION_DEMO } from './simule/pilotage'
import { CATALOGUE } from './simule/referentiels'
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
export type DemoNotifications = {
  recevoirMessage(o?: OptionsReceptionDemo): string
  /** Un autre membre lit la conversation (D-88) : accusé diffusé comme par le canal poussé ; vrai s'il a avancé. */
  lireMessages(o?: OptionsLectureDemo): boolean
  /** QUO-10 : un autre membre est en train d'écrire (« … écrit… » dans la conversation) ; rend la conversation. */
  ecrire(o?: { conversationId?: string; auteurId?: string }): string
}

/**
 * MS-NOTIFICATION simulé, branché sur la messagerie simulée : non-lus de démonstration (une notification
 * regroupée par conversation), une tâche confiée et un rapport reçu ; chaque message reçu crée ou regroupe une
 * notification et rafraîchit la messagerie. `window.gmDemoNotifications` déclenche une réception.
 */
export function notificationsDemo(services: ServicesSimules, o: { qc?: QueryClient; moi?: string; profils?: string[] } = {}): ServiceNotificationsSimule {
  const il = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString()
  const moi = o.moi ?? 'U-001'
  // DDR-20 : notifications de démonstration propres à l'utilisateur, jamais pour l'acteur lui-même (§6). Rapport reçu : d'un
  // membre de son équipe (il n'en reçoit pas sans équipe) ; tâche confiée : une tâche ouverte de sa liste, confiée par un
  // autre, et le lien mène à cette tâche ; action attendue d'homologation : à un porteur de la fonction concernée (SNA).
  const equipe = ORGANISATION_DEMO.filter((m) => m.responsableId === moi)
  const auteurRapport = equipe.find((m) => m.id === 'U-101') ?? equipe[0]
  const confiee = services.activiteDemo.tacheConfiee()
  const profilsSna = CATALOGUE.fonctions.find((f) => f.code === 'SNA')?.profils ?? []
  const sna = !o.profils || o.profils.some((p) => profilsSna.includes(p))
  const initiales: ArriveeNotification[] = [
    ...(auteurRapport ? [{ type: 'RAPPORT' as const, titre: `Rapport de ${auteurRapport.nomAffiche}`, sousTitre: 'Rapport quotidien', lien: '/des/rapports?vue=recus', reference: { service: 'pilotage', type: 'RAPPORT', id: 'RAP-DEMO' },
      acteur: { id: auteurRapport.id, nomAffiche: auteurRapport.nomAffiche }, le: il(26 * 60), lueLe: il(20 * 60) }] : []),
    ...(confiee ? [{ type: 'TACHE' as const, titre: `Tâche confiée par ${confiee.createur.nomAffiche}`, sousTitre: null, lien: `/des/taches?tache=${encodeURIComponent(confiee.id)}`,
      reference: { service: 'pilotage', type: 'TACHE', id: confiee.id }, acteur: confiee.createur, le: il(90) }] : []),
    // Parcours d'homologation (MS-NOTIFICATION §11) : action attendue sur une opération, lien profond vers l'onglet Opérations.
    ...(sna ? [{ type: 'HOMOLOGATION' as const, titre: 'Action attendue : Atelier comptabilisation', sousTitre: 'LIA-2026-036 (Établissement de microfinance C)',
      lien: '/des/parcours/LIA-2026-036?onglet=operations&operation=LIA-2026-036%3A11', reference: { service: 'homologation', type: 'OPERATION', id: 'LIA-2026-036:11' },
      acteur: null, le: il(40 * 60), lueLe: il(39 * 60) }] : []),
    ...services.messagerie.nonLuesDemo().map((r) => ({ ...arriveeMessage(r), nombre: r.nonLus })),
  ]
  // D-98 : décision à prononcer (transition de statut proposée) offerte à l'utilisateur : notification TACHE vers son écran,
  // datée de la proposition (DDR-15).
  const decision = (d: DecisionDemo): ArriveeNotification => ({
    type: 'TACHE', titre: d.titre, sousTitre: 'À prendre', lien: d.lien, reference: { service: 'pilotage', type: 'TACHE', id: d.tacheId },
    acteur: d.createur, le: d.le ?? il(0),
  })
  // DDR-08 : proposition de l'utilisateur prononcée ou refusée (participant.transition-*, MS-NOTIFICATION) : notification DEMANDE.
  const tranchee = (d: DecisionTrancheeDemo): ArriveeNotification => ({
    type: 'DEMANDE',
    titre: `${d.decision === 'REJETEE' ? 'Proposition de statut refusée' : d.vers === 'ADHERENT' ? 'Adhésion prononcée' : 'Transition de statut prononcée'} : ${d.institutionNom}`,
    sousTitre: d.decision === 'REJETEE' && d.motif ? `Motif : ${d.motif}` : null, lien: d.lien, reference: { service: 'participant', type: 'TRANSITION', id: d.transitionId },
    acteur: d.acteur, le: d.le,
  })
  // DDR-08 : opération ouverte ou renvoyée dont l'utilisateur est responsable (homologation.operation-activated.v1).
  const operation = (n: NotificationOperationDemo): ArriveeNotification => ({
    // HOM-16 : validation demandée au validateur désigné (« Validation attendue » de MS-NOTIFICATION, §11.1).
    type: 'HOMOLOGATION', titre: `${n.type === 'RENVOYEE' ? 'Opération renvoyée' : n.type === 'VALIDATION' ? 'Validation attendue' : 'Action attendue'} : ${n.libelle}`,
    sousTitre: n.type === 'RENVOYEE' && n.motif ? `${n.liaisonId} (${n.institutionNom}). Motif : ${n.motif}` : `${n.liaisonId} (${n.institutionNom})`,
    lien: n.lien, reference: { service: 'homologation', type: 'OPERATION', id: n.operationId }, acteur: null, le: n.le,
  })
  initiales.push(...services.decisions.aPrononcer().map(decision))
  initiales.push(...services.decisions.tranchees().map(tranchee))
  initiales.push(...services.evenementsParcours.pourMoi().map(operation))
  // QUO-05 : étapes de processus à réaliser, à prendre, à valider ou à signer, avancement et clôture des demandes de l'utilisateur.
  initiales.push(...(services.processusDemo?.pour(moi) ?? []))
  const notifications = notificationsSimulees({ initiales })
  services.decisions.surOffre((d) => notifications.recevoir(decision(d)))
  services.decisions.surTranchee((d) => notifications.recevoir(tranchee(d)))
  services.evenementsParcours.surEvenement((n) => notifications.recevoir(operation(n)))
  services.processusDemo?.surNotification((n) => {
    if (n.destinataire !== moi) return
    const { destinataire: _d, ...arrivee } = n
    notifications.recevoir(arrivee)
  })
  services.messagerie.surReception((r) => {
    notifications.recevoir(arriveeMessage(r))
    const mention = arriveeMention(r, moi)
    if (mention) notifications.recevoir(mention)
    void o.qc?.invalidateQueries({ queryKey: ['collaboration'] })
  })
  const demo: DemoNotifications = { recevoirMessage: (x) => services.messagerie.recevoirDemo(x).conversation.id, lireMessages: (x) => services.messagerie.lireDemo(x) !== null,
    ecrire: (x) => services.messagerie.ecrireDemo(x).conversationId }
  ;(window as unknown as { gmDemoNotifications?: DemoNotifications }).gmDemoNotifications = demo
  return notifications
}

/**
 * Notifications du portail de démonstration (MS-NOTIFICATION §11, CER-12) : comme le service, une action attendue par
 * opération en cours qui incombe à l'institution (signature du procès-verbal, attestations, coordonnées, livrables, fiche
 * de suivi), avec le lien du geste au portail (celui de la tâche), et le jalon atteint de ses liaisons. Les tâches sont
 * relues du parcours simulé au démarrage puis à intervalle régulier : chaque nouvelle tâche crée sa notification.
 */
export function notificationsPortailDemo(services?: Pick<ServicesSimules, 'portailParcours'>, o: { intervalleMs?: number } = {}): ServiceNotificationsSimule {
  const il = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString()
  const notifications = notificationsSimulees({
    couche: 'portail',
    initiales: [
      { type: 'HOMOLOGATION', titre: 'Jalon atteint : En production', sousTitre: 'LIA-2025-088 (Établissement de paiement A)',
        lien: '/portail/liaisons/LIA-2025-088', reference: { service: 'homologation', type: 'LIAISON', id: 'LIA-2025-088' }, acteur: null, le: il(26 * 60), lueLe: il(20 * 60) },
    ],
  })
  if (!services) return notifications
  const vues = new Set<string>()
  const synchroniser = () => services.portailParcours.mesTaches().then((t) => t.taches.forEach((x) => {
    if (vues.has(x.operationId)) return
    vues.add(x.operationId)
    notifications.recevoir({
      type: 'HOMOLOGATION', titre: `Action attendue : ${x.libelle ?? x.rang}`, sousTitre: x.echeance ? `${x.liaisonReference} (${x.institutionNom}), échéance ${x.echeance}` : `${x.liaisonReference} (${x.institutionNom})`,
      lien: x.lien, reference: { service: 'homologation', type: 'OPERATION', id: x.operationId }, acteur: null,
    })
  }), () => undefined)
  void synchroniser()
  if (o.intervalleMs !== 0) setInterval(() => void synchroniser(), o.intervalleMs ?? 15_000)
  return notifications
}
