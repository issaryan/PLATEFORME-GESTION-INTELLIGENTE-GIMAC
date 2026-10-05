package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.ConversationsPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution des conversations (développement, tests) : une conversation inconnue est
 * traitée comme directe (sans sous-titre) ; les groupes sont déclarés par les tests.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.conversations", havingValue = "stub", matchIfMissing = true)
public class ConversationsStubAdapter implements ConversationsPort {

    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    public void groupe(String id, String nom, List<String> membres) {
        conversations.put(id, new Conversation(id, GROUPE, nom, membres));
    }

    public void reinitialiser() {
        conversations.clear();
    }

    @Override
    public Optional<Conversation> conversation(String id) {
        return Optional.ofNullable(id == null ? null : conversations.get(id));
    }
}
