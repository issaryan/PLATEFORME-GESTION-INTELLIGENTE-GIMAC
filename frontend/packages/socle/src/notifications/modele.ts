// Notifications de la plateforme (D-80, docs/lot1/notifications.md) : représentations servies par
// MS-NOTIFICATION derrière la passerelle (/bff/{des,console}/notification). Aucun contenu de message n'y
// figure : l'aperçu d'un message est lu par l'interface auprès de la messagerie (membres seulement).

/**
 * MENTION et REPONSE (D-82) : « X vous a mentionné », « X a répondu à votre message », jamais regroupées.
 * DEMANDE (D-81) : demande adressée à l'administration, soumise ou décidée ; PROCESSUS (D-87) : étape d'un processus
 * d'unité activée, à signer ou à valider, instance close. Leur lien est rendu tel quel (préfixe console comme les autres).
 */
/** HOMOLOGATION (MS-NOTIFICATION §11) : action attendue, avis, validation ou jalon d'une opération du parcours d'homologation. */
export type TypeNotification = 'MESSAGE' | 'TACHE' | 'RAPPORT' | 'MENTION' | 'REPONSE' | 'DEMANDE' | 'PROCESSUS' | 'HOMOLOGATION'
export const TYPES_NOTIFICATION: readonly TypeNotification[] = ['MESSAGE', 'TACHE', 'RAPPORT', 'MENTION', 'REPONSE', 'DEMANDE', 'PROCESSUS', 'HOMOLOGATION']
/** Types qui portent sur la messagerie : leur lien mène à une conversation. */
export const TYPES_MESSAGERIE: readonly TypeNotification[] = ['MESSAGE', 'MENTION', 'REPONSE']

/** Objet visé : une conversation de MS-COLLABORATION, une tâche ou un rapport de MS-PILOTAGE. */
export type ReferenceNotification = { service: string; type: string; id: string }

export type NotificationPlateforme = {
  id: string
  type: TypeNotification
  /** Expéditeur, titre de la tâche ou « Rapport de … ». */
  titre: string
  /** Conversation de groupe ; échéance d'une tâche ; période d'un rapport. */
  sousTitre: string | null
  /** Chemin relatif dans la couche interne (/des/…) ; la console le transpose (liensNotification.ts). */
  lien: string
  reference: ReferenceNotification
  /** Conversation concernée (MESSAGE, MENTION, REPONSE, D-82) ; null pour une tâche ou un rapport. */
  conversationId?: string | null
  acteur: { id: string; nomAffiche: string } | null
  /** Messages regroupés dans cette notification (consolidation par conversation). */
  nombre: number
  creeLe: string
  misAJourLe: string
  lueLe: string | null
}

export type EtatNotifications = 'non-lues' | 'toutes'
export type PageNotifications = { contenu: NotificationPlateforme[]; total: number; page: number; taille: number }
export type PreferencesNotifications = { systeme: boolean; son: boolean; types: Record<TypeNotification, boolean> }
/** Corps de POST /notifications/lecture : toutes les notifications, celles d'un type ou celles d'un objet. */
export type FiltreLecture = { type?: TypeNotification; reference?: ReferenceNotification }

/**
 * Conversation d'une notification de la messagerie : `conversationId` (D-82), sinon la référence d'une
 * notification de message (antérieure à D-82) ; null hors messagerie.
 */
export function conversationDe(n: Pick<NotificationPlateforme, 'type' | 'reference' | 'conversationId'>): string | null {
  if (!TYPES_MESSAGERIE.includes(n.type)) return null
  if (n.conversationId) return n.conversationId
  return n.type === 'MESSAGE' && n.reference.type === 'CONVERSATION' ? n.reference.id : null
}

/** Référence d'une conversation de la messagerie (D-64), telle que MS-NOTIFICATION la porte. */
export const referenceConversation = (id: string): ReferenceNotification => ({ service: 'collaboration', type: 'CONVERSATION', id })

/** Port des notifications : MS-NOTIFICATION (http.ts) ou serveur de substitution (simule.ts). */
export interface ServiceNotifications {
  /** Les miennes, récentes d'abord (misAJourLe décroissant) ; page à partir de 0. */
  lister(q: { etat?: EtatNotifications; page?: number; taille?: number }): Promise<PageNotifications>
  compteur(): Promise<{ nonLues: number }>
  marquerLue(id: string): Promise<void>
  marquerLues(f?: FiltreLecture): Promise<void>
  preferences(): Promise<PreferencesNotifications>
  enregistrerPreferences(p: Partial<PreferencesNotifications>): Promise<PreferencesNotifications>
  /**
   * Arrivées locales (services simulés seulement) : avec les services réels, le canal poussé (sujet
   * « notification ») invalide les requêtes ; rend la fonction de désabonnement.
   */
  ecouter?(rappel: () => void): () => void
}
