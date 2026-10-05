package cm.gimac.des.notification.application.port.in;

import java.time.Instant;
import java.util.List;

/**
 * Événements des autres services transformés en notifications (docs/lot1/notifications.md §1) : une
 * notification par destinataire, jamais pour l'auteur ou l'acteur, selon ses préférences ; chaque
 * notification créée ou regroupée publie notification.inapp-created.v1 par l'outbox.
 */
public interface RecevoirEvenementsUseCase {
    /**
     * collaboration.message-sent.v1 (sujet collaboration.message).
     *
     * @param evenementGroupe événement de groupe (D-68) inscrit dans le fil : ignoré, seul un vrai message notifie
     */
    record MessageEnvoye(String conversationId, String messageId, String auteurId, List<String> destinataires, Instant envoyeLe, String evenementGroupe) {}

    /** pilotage.task-assigned.v1 (sujet pilotage.task) ; titre et échéance facultatifs (absents de la charge v1). */
    record TacheAffectee(String tacheId, String responsableId, String acteurId, Instant le, String titre, String echeance) {}

    /** pilotage.report-submitted.v1 (sujet pilotage.report). */
    record RapportSoumis(String rapportId, String type, String periode, String auteurId, String destinataireId, String acteurId, Instant le) {}

    /** Rend le nombre de notifications créées ou regroupées. */
    int messageEnvoye(MessageEnvoye e);

    int tacheAffectee(TacheAffectee e);

    int rapportSoumis(RapportSoumis e);

    /** Purge des notifications de plus de 90 jours (§2) ; rend le nombre supprimé. */
    int purger();
}
