package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.AnnuairePort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel de l'annuaire : interface interne de MS-IAM GET /api/v1/interne/collaborateurs/{id}
 * (jeton de service du client notification-service, INTERNAL_IAM_READ). Noms mis en cache
 * (gimac.notification.cache-noms-secondes, 300 s par défaut) ; compte inconnu : vide ; MS-IAM injoignable :
 * 503 NOT_ANNUAIRE_INDISPONIBLE, jamais mis en cache.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "real")
public class IamAnnuaireAdapter implements AnnuairePort {

    record CollaborateurIam(String id, String nomAffiche) {}

    private final RestClient client;
    private final CacheExpirant<String, Optional<String>> noms;

    public IamAnnuaireAdapter(
        RestClient.Builder builder,
        ServiceTokenInterceptor jeton,
        @Value("${gimac.services.iam.url}") String url,
        @Value("${gimac.notification.cache-noms-secondes:300}") long cacheSecondes
    ) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
        this.noms = new CacheExpirant<>(Duration.ofSeconds(cacheSecondes));
    }

    @Override
    public Optional<String> nomAffiche(String compteId) {
        return noms.lire(compteId, this::lire);
    }

    private Optional<String> lire(String id) {
        try {
            CollaborateurIam c = client.get().uri("/api/v1/interne/collaborateurs/{id}", id).retrieve().body(CollaborateurIam.class);
            return Optional.ofNullable(c).map(CollaborateurIam::nomAffiche).filter(n -> !n.isBlank());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            throw NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Annuaire des collaborateurs indisponible");
        }
    }
}
