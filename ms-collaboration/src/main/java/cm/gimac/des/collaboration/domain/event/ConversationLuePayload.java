package cm.gimac.des.collaboration.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de collaboration.conversation-read.v1 (D-88, sujet collaboration.message, clé = conversation) :
 * la position de lecture de {@code lecteurId} a avancé jusqu'au message envoyé à {@code luJusquA}. Publiée
 * seulement quand la position avance ; jamais de contenu.
 *
 * @param destinataires membres de la conversation sauf le lecteur
 */
public record ConversationLuePayload(UUID conversationId, String lecteurId, Instant luJusquA, List<String> destinataires) {
    public static final String TYPE = "collaboration.conversation-read.v1";

    public ConversationLuePayload {
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
