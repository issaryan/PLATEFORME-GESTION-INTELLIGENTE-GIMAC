package cm.gimac.des.collaboration.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de collaboration.message-sent.v1 (sujet collaboration.message, clé = conversation) :
 * destinataires à notifier en temps réel, jamais le contenu du message (D-64).
 */
public record MessageEnvoyePayload(UUID conversationId, UUID messageId, String auteurId, List<String> destinataires, Instant envoyeLe) {
    public static final String TYPE = "collaboration.message-sent.v1";
}
