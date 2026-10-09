package cm.gimac.des.iam.infrastructure.pdf;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.infrastructure.client.ServiceTokenInterceptor;
import java.net.http.HttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur réel du port {@link RenduPdf} (D-102, docs/lot1/organigramme.md §4) : route interne
 * {@code POST /api/v1/interne/rendus/pdf} de MS-DOCUMENT, jeton de service du client iam-service (permission
 * INTERNAL_DOCUMENT_RENDER), corps JSON {@code { html, piedDePage, format, orientation }}, réponse : le PDF. Toute
 * réponse autre qu'un PDF, délai dépassé (90 s) ou connexion impossible : 503 ORG_PDF_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.rendu-pdf", havingValue = "real")
public class RenduPdfDocument implements RenduPdf {

    private static final Logger LOG = LoggerFactory.getLogger(RenduPdfDocument.class);
    static final String ROUTE = "/api/v1/interne/rendus/pdf";

    /** Corps de la requête interne. */
    record Requete(String html, String piedDePage, String format, String orientation) {}

    private final RestClient client;

    public RenduPdfDocument(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(http);
        fabrique.setReadTimeout(Duration.ofSeconds(90));
        this.client = builder.baseUrl(url).requestFactory(fabrique).requestInterceptor(jeton).build();
    }

    @Override
    public byte[] rendre(String html, String piedDePage, Mise mise) {
        Requete corps = new Requete(html, piedDePage, mise.format().name(), mise.paysage() ? "PAYSAGE" : "PORTRAIT");
        try {
            return client
                .post()
                .uri(ROUTE)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_PDF)
                .body(corps)
                .exchange((requete, reponse) -> {
                    if (reponse.getStatusCode().is2xxSuccessful()) {
                        byte[] pdf = reponse.getBody().readAllBytes();
                        if (pdf.length > 4 && pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F') {
                            return pdf;
                        }
                    }
                    LOG.warn("Mise en page PDF : réponse {} de MS-DOCUMENT", reponse.getStatusCode().value());
                    throw indisponible();
                });
        } catch (IamException e) {
            throw e;
        } catch (RestClientException | IllegalStateException e) {
            LOG.warn("Mise en page PDF : MS-DOCUMENT injoignable ou délai dépassé ({})", e.getClass().getSimpleName());
            throw indisponible();
        }
    }

    static IamException indisponible() {
        return IamException.indisponible("ORG_PDF_INDISPONIBLE", "Le moteur de mise en page PDF est momentanément indisponible ; réessayer dans quelques instants");
    }
}
