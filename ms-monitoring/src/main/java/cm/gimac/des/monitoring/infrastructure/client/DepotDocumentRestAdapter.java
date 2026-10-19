package cm.gimac.des.monitoring.infrastructure.client;

import cm.gimac.des.monitoring.application.port.out.DepotDocumentPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel du dépôt (MS-DOCUMENT, jeton de service monitoring-service, INTERNAL_DOCUMENT_DEPOSIT) :
 * POST /api/v1/interne/documents en multipart (fichier, finalite=RAPPORT_QUART, reference, deposantId). MS-DOCUMENT
 * contrôle le masquage et l'empreinte et rend l'identifiant du document. Injoignable : 503 MON_DEPOT_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.documents", havingValue = "real")
public class DepotDocumentRestAdapter implements DepotDocumentPort {

    static final String FINALITE = "RAPPORT_QUART";

    record Depot(String id) {}

    private final RestClient client;

    public DepotDocumentRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public String deposer(byte[] contenu, String nom, String typeMime, String empreinte) {
        try {
            MultipartBodyBuilder corps = new MultipartBodyBuilder();
            corps
                .part("fichier", new ByteArrayResource(contenu) {
                    @Override
                    public String getFilename() {
                        return nom;
                    }
                })
                .contentType(MediaType.parseMediaType(typeMime))
                .filename(nom);
            corps.part("finalite", FINALITE);
            corps.part("reference", nom);
            corps.part("deposantId", "monitoring");
            Depot d = client.post().uri("/api/v1/interne/documents").contentType(MediaType.MULTIPART_FORM_DATA).body(corps.build()).retrieve().body(Depot.class);
            if (d == null || d.id() == null) {
                throw MonitoringException.indisponible("MON_DEPOT_INDISPONIBLE", "Dépôt (MS-DOCUMENT) indisponible");
            }
            return d.id();
        } catch (RestClientException | IllegalStateException e) {
            throw MonitoringException.indisponible("MON_DEPOT_INDISPONIBLE", "Dépôt (MS-DOCUMENT) indisponible");
        }
    }
}
