package cm.gimac.des.collaboration.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Signal « en train d'écrire » (D-72), éphémère : jamais enregistré, ni dans l'outbox ni par la
 * passerelle, qui ne le place pas dans son tampon de reprise.
 */
public record SaisiePayload(UUID conversationId, String auteurId, List<String> destinataires, Instant le) {
    public static final String TYPE = "collaboration.message-typing.v1";
}
