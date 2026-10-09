package cm.gimac.des.iam.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.port.out.RenduPdf;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.infrastructure.client.ServiceTokenInterceptor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestClient;

/**
 * Adaptateur réel du rendu PDF (D-102 §4) contre une fausse route interne de MS-DOCUMENT (serveur HTTP du JDK) :
 * requête JSON attendue avec le jeton de service, PDF rendu ; toute autre réponse, connexion impossible : 503
 * ORG_PDF_INDISPONIBLE. Le substitut refuse toujours par 503.
 */
class RenduPdfDocumentTest {

    static final byte[] PDF = "%PDF-1.7 rendu".getBytes(StandardCharsets.US_ASCII);

    HttpServer serveur;
    final AtomicReference<String> chemin = new AtomicReference<>();
    final AtomicReference<String> jeton = new AtomicReference<>();
    final AtomicReference<String> corps = new AtomicReference<>();
    volatile int statut = 200;
    volatile byte[] reponse = PDF;

    @BeforeEach
    void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", this::repondre);
        serveur.start();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    void repondre(HttpExchange e) throws IOException {
        chemin.set(e.getRequestMethod() + " " + e.getRequestURI().getPath());
        jeton.set(e.getRequestHeaders().getFirst("Authorization"));
        corps.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        e.sendResponseHeaders(statut, reponse.length);
        e.getResponseBody().write(reponse);
        e.close();
    }

    static RenduPdfDocument adaptateur(String url) {
        OAuth2AuthorizedClientManager gestionnaire = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId("service-token")
            .clientId("iam-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak/token")
            .build();
        OAuth2AccessToken acces = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-de-service", Instant.now(), Instant.now().plusSeconds(60));
        when(gestionnaire.authorize(any())).thenReturn(new OAuth2AuthorizedClient(enregistrement, "service", acces));
        return new RenduPdfDocument(RestClient.builder(), new ServiceTokenInterceptor(gestionnaire), url);
    }

    String url() {
        return "http://127.0.0.1:" + serveur.getAddress().getPort();
    }

    @Test
    @DisplayName("PDF rendu : POST /api/v1/interne/rendus/pdf, jeton de service, document, pied de page, format et orientation")
    void rendu() {
        byte[] pdf = adaptateur(url()).rendre("<html>é</html>", "<p>pied</p>", new RenduPdf.Mise(RenduPdf.Format.A3, true));
        assertThat(pdf).isEqualTo(PDF);
        assertThat(chemin.get()).isEqualTo("POST /api/v1/interne/rendus/pdf");
        assertThat(jeton.get()).isEqualTo("Bearer jeton-de-service");
        assertThat(corps.get()).contains("\"html\":\"<html>é</html>\"").contains("\"piedDePage\":\"<p>pied</p>\"").contains("\"format\":\"A3\"").contains("\"orientation\":\"PAYSAGE\"");
        adaptateur(url()).rendre("<html></html>", null, new RenduPdf.Mise(RenduPdf.Format.A4, false));
        assertThat(corps.get()).contains("\"orientation\":\"PORTRAIT\"");
    }

    @Test
    @DisplayName("Refus ou panne de MS-DOCUMENT, réponse qui n'est pas un PDF, connexion impossible : 503 ORG_PDF_INDISPONIBLE")
    void indisponible() throws IOException {
        statut = 503;
        reponse = "{\"code\":\"DOC_RENDU_INDISPONIBLE\"}".getBytes(StandardCharsets.UTF_8);
        indisponible(adaptateur(url()));
        statut = 200;
        reponse = "<html>pas un pdf</html>".getBytes(StandardCharsets.UTF_8);
        indisponible(adaptateur(url()));
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        indisponible(adaptateur("http://127.0.0.1:" + port));
        indisponible(new RenduPdfIndisponible());
    }

    static void indisponible(RenduPdf a) {
        assertThatThrownBy(() -> a.rendre("<html></html>", null, new RenduPdf.Mise(RenduPdf.Format.A3, true))).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("ORG_PDF_INDISPONIBLE");
            assertThat(e.status()).isEqualTo(503);
        });
    }
}
