package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel : interface interne de MS-PILOTAGE ({@code POST /api/v1/interne/taches}, {@code /cloture},
 * {@code /reouverture}), jeton de service portant INTERNAL_PILOTAGE_TASK_CREATE. Un refus 4xx est définitif ;
 * une indisponibilité (5xx, réseau) est réessayée par le relais.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.pilotage-tasks", havingValue = "real")
public class PilotageTachesRestAdapter implements PilotageTachesPort {

    private final RestClient client;

    public PilotageTachesRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.pilotage.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Resultat creer(Tache t) {
        return appeler("/api/v1/interne/taches", t);
    }

    @Override
    public Resultat cloturer(Designation d) {
        return appeler("/api/v1/interne/taches/cloture", d);
    }

    @Override
    public Resultat rouvrir(Designation d) {
        return appeler("/api/v1/interne/taches/reouverture", d);
    }

    private Resultat appeler(String chemin, Object corps) {
        try {
            client.post().uri(chemin).contentType(MediaType.APPLICATION_JSON).body(corps).retrieve().toBodilessEntity();
            return Resultat.FAIT;
        } catch (HttpClientErrorException e) {
            return e.getStatusCode().value() == 401 || e.getStatusCode().value() == 429 ? Resultat.INDISPONIBLE : Resultat.REFUSE;
        } catch (RestClientException | IllegalStateException e) {
            return Resultat.INDISPONIBLE;
        }
    }
}
