package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.PiecesPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel des pièces (MS-DOCUMENT, jeton de service monitoring-service, INTERNAL_DOCUMENT_READ) :
 * GET /api/v1/interne/documents/{id} (métadonnées ; 404 : absente) et GET /api/v1/interne/documents/{id}/contenu.
 * MS-DOCUMENT injoignable : 503 MON_DEPOT_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "real")
public class PiecesRestAdapter implements PiecesPort {

    record Metadonnees(String id, String nomFichier, Long taille, String typeMime, String finalite, String deposant) {}

    private final RestClient client;

    public PiecesRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public Optional<Piece> lire(String id) {
        try {
            Metadonnees m = client.get().uri("/api/v1/interne/documents/{id}", id).accept(MediaType.APPLICATION_JSON).retrieve().body(Metadonnees.class);
            return Optional.ofNullable(m).map(x -> new Piece(x.id(), x.nomFichier(), x.taille() == null ? 0 : x.taille(), x.typeMime(), x.finalite(), x.deposant()));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 400) {
                return Optional.empty();
            }
            throw MonitoringException.indisponible("MON_DEPOT_INDISPONIBLE", "Pièces (MS-DOCUMENT) indisponibles");
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_DEPOT_INDISPONIBLE", "Pièces (MS-DOCUMENT) indisponibles");
        }
    }

    @Override
    public byte[] contenu(String id) {
        try {
            byte[] b = client.get().uri("/api/v1/interne/documents/{id}/contenu", id).retrieve().body(byte[].class);
            return b == null ? new byte[0] : b;
        } catch (HttpClientErrorException.NotFound e) {
            throw MonitoringException.introuvable("MON_PIECE_INTROUVABLE", id);
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_DEPOT_INDISPONIBLE", "Pièces (MS-DOCUMENT) indisponibles");
        }
    }
}
