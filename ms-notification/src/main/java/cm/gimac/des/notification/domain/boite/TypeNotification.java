package cm.gimac.des.notification.domain.boite;

/** Type d'une notification de la plateforme (docs/lot1/notifications.md §2). */
public enum TypeNotification {
    /** Message de la messagerie instantanée (collaboration.message-sent.v1). */
    MESSAGE,
    /** Tâche confiée (pilotage.task-assigned.v1). */
    TACHE,
    /** Rapport soumis (pilotage.report-submitted.v1). */
    RAPPORT,
}
