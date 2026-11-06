package cm.gimac.des.iam.infrastructure.client;

import cm.gimac.des.iam.application.port.out.sandbox.RegistreInstitutions;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Correspondance du registre lue dans MS-PARTICIPANT (D-134) : {@code GET /api/v1/interne/institutions/{id}/correspondance},
 * jeton de service du client iam-service (INTERNAL_PARTICIPANT_READ). 404 : institution inconnue ; toute autre erreur,
 * délai dépassé ou connexion impossible : registre indisponible.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.registre-institutions", havingValue = "real")
public class RegistreInstitutionsParticipant implements RegistreInstitutions {

    static final String ROUTE = "/api/v1/interne/institutions/{id}/correspondance";
    static final String ROUTE_CODE = "/api/v1/interne/institutions/code/{code}/correspondance";

    private final RestClient client;

    public RegistreInstitutionsParticipant(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.participant.url}") String url) {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(Duration.ofSeconds(3));
        fabrique.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.baseUrl(url).requestFactory(fabrique).requestInterceptor(jeton).build();
    }

    RegistreInstitutionsParticipant(RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<String> codeParticipant(UUID institutionId) {
        return lire(ROUTE, institutionId, "codeParticipant");
    }

    @Override
    public Optional<UUID> identifiant(String codeParticipant) {
        return lire(ROUTE_CODE, codeParticipant, "institutionId").map(UUID::fromString);
    }

    private Optional<String> lire(String route, Object variable, String champ) {
        try {
            Map<?, ?> r = client.get().uri(route, variable).retrieve().body(Map.class);
            Object v = r == null ? null : r.get(champ);
            return v == null ? Optional.empty() : Optional.of(v.toString());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw new RegistreIndisponible("MS-PARTICIPANT répond " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new RegistreIndisponible("MS-PARTICIPANT injoignable", e);
        }
    }
}
