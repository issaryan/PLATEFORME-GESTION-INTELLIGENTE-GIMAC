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
    /**
     * Programmes et rapports de monitoring et d'astreinte (D-116, docs/lot1/astreintes-monitoring.md §10, sujet
     * monitoring.notification) : rapport reçu, rapport quotidien de 8 h attendu ou en retard, grille publiée, relève ;
     * une notification par événement et par destinataire, jamais regroupée.
     */
    MONITORING,
    /**
     * Sandbox GIMAC (US-EXI-09, docs/lot1/sandbox-existant-adapte.md §11, sujets sbx-*.notification-requested.v1) :
     * invitations, suspension et rétablissement d'un espace, mandats, échéances de certificat, campagnes, signalements
     * et alertes ; une notification par demande et par destinataire, jamais regroupée.
     */
    SANDBOX;

    /**
     * Types qu'un compte d'institution (portail) peut recevoir : le parcours d'homologation de sa liaison et les avis de
     * la sandbox ; messagerie, tâches, rapports, demandes et processus d'unité sont des fonctions internes du GIMAC.
     */
    public static final java.util.Set<TypeNotification> PORTAIL = java.util.EnumSet.of(HOMOLOGATION, SANDBOX);
}
