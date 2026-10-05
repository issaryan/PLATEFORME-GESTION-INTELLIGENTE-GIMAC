package cm.gimac.des.notification.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Conversations de MS-COLLABORATION (GET /api/v1/interne/conversations/{id}, jeton de service
 * INTERNAL_COLLABORATION_READ) : type et nom d'un groupe pour le sous-titre d'une notification de message.
 * Conversation inconnue : vide ; service injoignable : NOT_COLLABORATION_INDISPONIBLE (503).
 */
public interface ConversationsPort {
    String GROUPE = "GROUPE";

    record Conversation(String id, String type, String nom, List<String> membres) {
        public Conversation {
            membres = membres == null ? List.of() : List.copyOf(membres);
        }

        public boolean estGroupe() {
            return GROUPE.equals(type);
        }
    }

    Optional<Conversation> conversation(String id);
}
