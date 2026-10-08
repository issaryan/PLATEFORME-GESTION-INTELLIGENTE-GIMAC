package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.ComptesInstitutionPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : comptes d'une institution chez MS-IAM, {@code GET /api/v1/interne/collaborateurs?institution=<id>}
 * (jeton de service du client notification-service, INTERNAL_IAM_READ), contrat attendu de MS-IAM (D-93 §11) : tableau
 * de collaborateurs {@code { id, institutionId, actif, … }}. Par prudence, seuls les comptes actifs dont
 * {@code institutionId} est l'institution demandée sont retenus, que la réponse soit un tableau ou une page
 * ({@code { contenu: [...] }}) : un MS-IAM qui ignorerait le paramètre (annuaire interne) ne fait notifier personne.
 * Mise en cache 60 s ({@code gimac.notification.cache-institutions-secondes}) ; 404 : aucun compte ; autre erreur :
 * 503 NOT_ANNUAIRE_INDISPONIBLE, jamais mis en cache.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "real")
public class IamComptesInstitutionAdapter implements ComptesInstitutionPort {

    private final RestClient client;
    private final CacheExpirant<String, List<String>> comptes;

    public IamComptesInstitutionAdapter(
        RestClient.Builder builder,
        ServiceTokenInterceptor jeton,
        @Value("${gimac.services.iam.url}") String url,
        @Value("${gimac.notification.cache-institutions-secondes:60}") long cacheSecondes
    ) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
        this.comptes = new CacheExpirant<>(Duration.ofSeconds(cacheSecondes));
    }

    @Override
    public List<String> comptes(String institutionId) {
        if (institutionId == null || institutionId.isBlank()) {
            return List.of();
        }
        return comptes.lire(institutionId.strip(), this::lire);
    }

    private List<String> lire(String institutionId) {
        try {
            JsonNode r = client.get().uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam("institution", institutionId).build()).retrieve().body(JsonNode.class);
            return retenus(r, institutionId);
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (RestClientException | IllegalStateException e) {
            throw NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Comptes de l'institution indisponibles");
        }
    }

    /** Comptes actifs de l'institution, dans l'ordre de la réponse, sans doublon. */
    static List<String> retenus(JsonNode reponse, String institutionId) {
        JsonNode liste = reponse == null ? null : reponse.isArray() ? reponse : reponse.path("contenu");
        List<String> r = new ArrayList<>();
        if (liste != null && liste.isArray()) {
            for (JsonNode c : liste) {
                String id = c.path("id").asText("");
                if (!id.isBlank() && c.path("actif").asBoolean(false) && institutionId.equals(c.path("institutionId").asText(null)) && !r.contains(id)) {
                    r.add(id);
                }
            }
        }
        return List.copyOf(r);
    }
}
