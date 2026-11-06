package cm.gimac.des.pilotage.infrastructure.client;

import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** POST /api/v1/interne/rendus/pdf de MS-DOCUMENT, avec le jeton pilotage-service, au gabarit commun (D-139). */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.rendu-pdf", havingValue = "real")
public class RenduPdfRestAdapter implements RenduPdfPort {

    record Requete(String html, String piedDePage, String format, String orientation, Gabarit gabarit) {}

    /** En-tête et pied du gabarit commun de MS-DOCUMENT ; date d'édition ISO 8601. */
    record Gabarit(String titre, String sousTitre, String reference, String classification, String langue, String edition, List<Fait> faits) {}

    /** Corps de la requête interne : A4 portrait, pied composé par le gabarit. */
    static Requete requete(Document d) {
        return new Requete(
            d.corps(),
            null,
            "A4",
            "PORTRAIT",
            new Gabarit(d.titre(), d.sousTitre(), d.reference(), d.classification(), d.langue(), d.edition().atOffset(ZoneOffset.UTC).toString(), d.faits())
        );
    }

    private final RestClient client;

    public RenduPdfRestAdapter(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        this.client = builder.baseUrl(url).requestInterceptor(jeton).build();
    }

    @Override
    public byte[] rendre(Document document) {
        try {
            byte[] pdf = client.post().uri("/api/v1/interne/rendus/pdf").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PDF)
                .body(requete(document)).retrieve().body(byte[].class);
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
