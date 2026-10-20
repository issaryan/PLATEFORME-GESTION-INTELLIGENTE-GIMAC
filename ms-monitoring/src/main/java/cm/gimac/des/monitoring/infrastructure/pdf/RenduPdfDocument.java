package cm.gimac.des.monitoring.infrastructure.pdf;

import cm.gimac.des.monitoring.application.port.out.RenduPdfPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.infrastructure.client.ServiceTokenInterceptor;
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
 * Adaptateur réel de {@link RenduPdfPort} (D-117) : route interne {@code POST /api/v1/interne/rendus/pdf} de MS-DOCUMENT,
 * jeton de service de monitoring-service, corps {@code { html, piedDePage, format, orientation }}, réponse : le PDF.
 * Toute réponse autre qu'un PDF, délai dépassé (90 s) ou connexion impossible : 503 MON_PDF_INDISPONIBLE.
 */
@Component
@ConditionalOnProperty(name = "gimac.adaptateurs.rendu-pdf", havingValue = "real")
public class RenduPdfDocument implements RenduPdfPort {

    private static final Logger LOG = LoggerFactory.getLogger(RenduPdfDocument.class);
    static final String ROUTE = "/api/v1/interne/rendus/pdf";

    record Requete(String html, String piedDePage, String format, String orientation) {}

    private final RestClient client;

    public RenduPdfDocument(RestClient.Builder builder, ServiceTokenInterceptor jeton, @Value("${gimac.services.document.url}") String url) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(http);
        fabrique.setReadTimeout(Duration.ofSeconds(90));
        this.client = builder.baseUrl(url).requestFactory(fabrique).requestInterceptor(jeton).build();
    }

    @Override
    public byte[] rendre(String html, String piedDePage, boolean paysage) {
        Requete corps = new Requete(html, piedDePage, "A4", paysage ? "PAYSAGE" : "PORTRAIT");
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
                    LOG.warn("Programme en PDF : réponse {} de MS-DOCUMENT", reponse.getStatusCode().value());
                    throw indisponible();
                });
        } catch (MonitoringException e) {
            throw e;
        } catch (RestClientException | IllegalStateException e) {
            LOG.warn("Programme en PDF : MS-DOCUMENT injoignable ou délai dépassé ({})", e.getClass().getSimpleName());
            throw indisponible();
        }
    }

    static MonitoringException indisponible() {
        return MonitoringException.indisponible("MON_PDF_INDISPONIBLE", "Le moteur de mise en page PDF est momentanément indisponible ; réessayer dans quelques instants");
    }
}
