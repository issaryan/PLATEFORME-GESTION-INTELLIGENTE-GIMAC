package cm.gimac.des.collaboration.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de collaboration.message-sent.v1 (sujet collaboration.message, clé = conversation) :
 * destinataires à notifier en temps réel, jamais le contenu du message (D-64).
 *
 * @param evenement événement de groupe inscrit dans le fil (D-68 : GROUPE_CREE, GROUPE_RENOMME,
 *        MEMBRES_AJOUTES, MEMBRE_RETIRE, MEMBRE_PARTI), null pour un vrai message ; MS-NOTIFICATION
 *        ne notifie que les vrais messages (D-80, docs/lot1/notifications.md §1)
 */
public record MessageEnvoyePayload(UUID conversationId, UUID messageId, String auteurId, List<String> destinataires, Instant envoyeLe, String evenement) {
    public static final String TYPE = "collaboration.message-sent.v1";

    /** Vrai message (sans événement de groupe). */
    public MessageEnvoyePayload(UUID conversationId, UUID messageId, String auteurId, List<String> destinataires, Instant envoyeLe) {
        this(conversationId, messageId, auteurId, destinataires, envoyeLe, null);
    }
}
