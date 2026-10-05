package cm.gimac.des.collaboration.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Lecture d'une conversation par un autre service (route interne, jeton de service portant
 * INTERNAL_COLLABORATION_READ, D-80) : type, nom d'un groupe et identifiants des membres, jamais de
 * message ni de pièce. MS-NOTIFICATION s'en sert pour le sous-titre d'une notification de groupe.
 */
public interface ConsulterConversationInterneUseCase {
    /** {@code nom} : titre d'un groupe, null pour une conversation directe. */
    record ConversationInterne(UUID id, String type, String nom, List<String> membres) {
        public ConversationInterne {
            membres = List.copyOf(membres);
        }
    }

    /** Conversation connue ; sinon COL_CONVERSATION_NOT_FOUND (404). */
    ConversationInterne conversationInterne(UUID conversationId);
}
