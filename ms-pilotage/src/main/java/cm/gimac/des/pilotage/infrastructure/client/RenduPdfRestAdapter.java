package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** POST /api/v1/interne/rendus/pdf de MS-DOCUMENT, avec le jeton pilotage-service. */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.rendu-pdf", havingValue = "real")
public class RenduPdfRestAdapter implements RenduPdfPort {

    record Requete(String html, String piedDePage, String format, String orientation) {}

    private final RestClient client;

    public RenduPdfRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public byte[] rendre(String html, String piedDePage) {
        try {
            byte[] pdf = client.post().uri("/api/v1/interne/rendus/pdf").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PDF)
                .body(new Requete(html, piedDePage, "A4", "PORTRAIT")).retrieve().body(byte[].class);
            byte[] reponse = java.util.Optional.ofNullable(pdf).orElse(new byte[0]);
            if (reponse.length < 5 || !"%PDF".equals(new String(reponse, 0, 4, java.nio.charset.StandardCharsets.US_ASCII))) {
                throw indisponible();
            }
            return reponse;
        } catch (RestClientException | IllegalStateException e) {
            throw indisponible();
        }
    }

    private static PilotageException indisponible() {
        return PilotageException.indisponible("PIL_PDF_INDISPONIBLE", "Le moteur PDF de MS-DOCUMENT est indisponible");
    }
}
