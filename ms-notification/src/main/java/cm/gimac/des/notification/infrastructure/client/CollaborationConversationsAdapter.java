package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.ConversationsPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel des conversations : interface interne de MS-COLLABORATION
 * GET /api/v1/interne/conversations/{id} → { id, type, nom, membres } (jeton de service du client
 * notification-service, INTERNAL_COLLABORATION_READ). Réponses mises en cache
 * (gimac.notification.cache-conversations-secondes, 60 s : un groupe renommé apparaît vite) ; inconnue :
 * vide ; MS-COLLABORATION injoignable : 503 NOT_COLLABORATION_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.conversations", havingValue = "real")
public class CollaborationConversationsAdapter implements ConversationsPort {

    record ConversationCollaboration(String id, String type, String nom, List<String> membres) {}

    private final RestClient client;
    private final CacheExpirant<String, Optional<Conversation>> cache;

    public CollaborationConversationsAdapter(
        RestClient.Builder builder,
        ServiceTokenInterceptor jeton,
        @Value("${gimac.services.collaboration.url}") String url,
        @Value("${gimac.notification.cache-conversations-secondes:60}") long cacheSecondes
    ) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
        this.cache = new CacheExpirant<>(Duration.ofSeconds(cacheSecondes));
    }

    @Override
    public Optional<Conversation> conversation(String id) {
        return cache.lire(id, this::lire);
    }

    private Optional<Conversation> lire(String id) {
        try {
            ConversationCollaboration c = client.get().uri("/api/v1/interne/conversations/{id}", id).retrieve().body(ConversationCollaboration.class);
            return Optional.ofNullable(c).map(x -> new Conversation(x.id(), x.type(), x.nom(), x.membres()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw NotificationException.indisponible("NOT_COLLABORATION_INDISPONIBLE", "Messagerie indisponible");
        }
    }
}
