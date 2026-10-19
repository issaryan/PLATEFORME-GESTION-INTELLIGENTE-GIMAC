package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel de l'annuaire (MS-IAM, jeton de service monitoring-service, INTERNAL_IAM_READ) :
 * GET /api/v1/interne/collaborateurs?permission= (porteurs d'une permission) et
 * GET /api/v1/interne/collaborateurs/{id} (nom affiché, mis en cache 5 min). MS-IAM injoignable : 503
 * MON_ANNUAIRE_INDISPONIBLE ; un compte inconnu (404) est simplement absent.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.annuaire", havingValue = "real")
public class AnnuaireIamAdapter implements AnnuairePort {

    record CollaborateurIam(String id, String nomAffiche, Boolean actif, String institutionId) {}

    private static final ParameterizedTypeReference<List<CollaborateurIam>> LISTE = new ParameterizedTypeReference<>() {};
    private static final long CACHE_MS = 300_000L;

    private record Nom(String nom, long le) {}

    private final RestClient client;
    private final Map<String, Nom> cache = new ConcurrentHashMap<>();

    public AnnuaireIamAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.iam.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public List<Personne> porteurs(String permission) {
        try {
            List<CollaborateurIam> l = client
                .get()
                .uri(u -> u.path("/api/v1/interne/collaborateurs").queryParam("permission", permission).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(LISTE);
            return Optional.ofNullable(l)
                .orElse(List.of())
                .stream()
                .filter(c -> c.id() != null && !Boolean.FALSE.equals(c.actif()) && (c.institutionId() == null || c.institutionId().isBlank()))
                .map(c -> new Personne(c.id(), c.nomAffiche() == null ? c.id() : c.nomAffiche()))
                .sorted(Comparator.comparing(Personne::nom))
                .toList();
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_ANNUAIRE_INDISPONIBLE", "Annuaire (MS-IAM) indisponible");
        }
    }

    @Override
    public Map<String, String> noms(Collection<String> ids) {
        Map<String, String> r = new HashMap<>();
        long maintenant = System.currentTimeMillis();
        for (String id : new HashSet<>(ids)) {
            if (id == null) {
                continue;
            }
            Nom n = cache.get(id);
            if (n == null || maintenant - n.le() > CACHE_MS) {
                n = new Nom(lire(id), maintenant);
                cache.put(id, n);
            }
            if (n.nom() != null) {
                r.put(id, n.nom());
            }
        }
        return r;
    }

    private String lire(String id) {
        try {
            CollaborateurIam c = client.get().uri("/api/v1/interne/collaborateurs/{id}", id).accept(MediaType.APPLICATION_JSON).retrieve().body(CollaborateurIam.class);
            return c == null ? null : c.nomAffiche();
        } catch (HttpClientErrorException e) {
            return null;
        } catch (RestClientException | IllegalStateException e) {
            return null;
        }
    }
}
