package cm.gimac.des.notification.infrastructure.client;

import cm.gimac.des.notification.application.port.out.DestinatairesSandboxPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel des destinataires de la sandbox (US-EXI-09) : {@code GET /api/v1/interne/collaborateurs?institution=}
 * et {@code ?permission=} de MS-IAM (jeton de service du client notification-service). Seuls les comptes actifs sont
 * retenus, et pour une institution ceux dont {@code institutionId} est le code demandé. Cache de 60 s ; 404 : aucun
 * compte ; autre erreur : 503 NOT_ANNUAIRE_INDISPONIBLE, jamais mis en cache.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.annuaire", havingValue = "real")
public class IamDestinatairesSandboxAdapter implements DestinatairesSandboxPort {

    private final RestClient client;
    private final CacheExpirant<String, Map<String, Set<String>>> institutions;
    private final CacheExpirant<String, List<String>> porteurs;

    @Autowired
    public IamDestinatairesSandboxAdapter(
        RestClient.Builder builder,
        ServiceTokenInterceptor jeton,
        @Value("${gimac.services.iam.url}") String url,
        @Value("${gimac.notification.cache-institutions-secondes:60}") long cacheSecondes
    ) {
        this(builder.baseUrl(url).requestInterceptor(jeton).build(), cacheSecondes);
    }

    IamDestinatairesSandboxAdapter(RestClient client, long cacheSecondes) {
        this.client = client;
        this.institutions = new CacheExpirant<>(Duration.ofSeconds(cacheSecondes));
        this.porteurs = new CacheExpirant<>(Duration.ofSeconds(cacheSecondes));
    }

    @Override
    public Map<String, Set<String>> comptesInstitution(String codeParticipant) {
        if (codeParticipant == null || codeParticipant.isBlank()) {
            return Map.of();
        }
        String code = codeParticipant.strip();
        return institutions.lire(code, c -> profils(lire("institution", c), c));
    }

    @Override
    public List<String> porteurs(String permission) {
        return porteurs.lire(permission, p -> new ArrayList<>(profils(lire("permission", p), null).keySet()));
    }

    private JsonNode lire(String parametre, String valeur) {
        try {
            return client.get().uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam(parametre, valeur).build()).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (RestClientException | IllegalStateException e) {
            throw NotificationException.indisponible("NOT_ANNUAIRE_INDISPONIBLE", "Annuaire des comptes indisponible");
        }
    }

    /** Comptes actifs de la réponse (tableau ou page), et leurs profils ; institution exigée si non nulle. */
    static Map<String, Set<String>> profils(JsonNode reponse, String institution) {
        JsonNode liste = reponse == null ? null : reponse.isArray() ? reponse : reponse.path("contenu");
        Map<String, Set<String>> r = new LinkedHashMap<>();
        if (liste != null && liste.isArray()) {
            for (JsonNode c : liste) {
                String id = c.path("id").asText("");
                boolean admis = !id.isBlank() && c.path("actif").asBoolean(false) && (institution == null || institution.equals(c.path("institutionId").asText(null)));
                if (admis) {
                    Set<String> p = new LinkedHashSet<>();
                    c.path("profils").forEach(x -> p.add(x.asText()));
                    r.putIfAbsent(id, Set.copyOf(p));
                }
            }
        }
        return r;
    }
}
