package cm.gimac.des.notification.domain.boite;

/** Type d'une notification de la plateforme (docs/lot1/notifications.md §2). */
public enum TypeNotification {
    /** Message de la messagerie instantanée (collaboration.message-sent.v1). */
    MESSAGE,
    /** Tâche confiée, à prendre ou relancée (pilotage.task-assigned.v1, task-offered.v1, task-reminded.v1). */
    TACHE,
    /** Rapport soumis (pilotage.report-submitted.v1). */
    RAPPORT,
    /**
     * Demande à l'administration soumise ou décidée (iam.request-submitted.v1, iam.request-decided.v1,
     * docs/lot1/pouvoirs-hierarchiques.md §4).
     */
    DEMANDE,
    /**
     * Mention d'un membre dans un message de la messagerie (D-82, collaboration.message-sent.v1, champ
     * mentionnes) : une notification par message, jamais regroupée.
     */
    MENTION,
    /**
     * Réponse à un message du destinataire (D-82, collaboration.message-sent.v1, champ reponseAAuteurId) :
     * une notification par message, jamais regroupée.
     */
    REPONSE,
    /**
     * Processus d'unité (D-87, docs/lot1/processus-unite.md §5.5, sujet pilotage.process) : instance lancée, étape
     * activée, soumise, signée ou réaffectée, instance close ou annulée ; une notification par événement et par
     * destinataire, jamais regroupée.
     */
    PROCESSUS,
    /**
     * Parcours d'homologation (D-93, docs/lot1/effet-reel-referentiels.md §8.2 et §11, sujets homologation.operation et
     * homologation.liaison) : opération ouverte ou en validation (action attendue, avis demandé, information, escalade
     * selon la qualité RACI), jalon atteint ; une notification par événement et par destinataire, jamais regroupée.
     */
    HOMOLOGATION,
}
