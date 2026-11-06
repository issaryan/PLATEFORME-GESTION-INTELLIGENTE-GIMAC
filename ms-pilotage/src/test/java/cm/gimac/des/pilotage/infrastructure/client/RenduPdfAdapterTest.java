package cm.gimac.des.pilotage.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import cm.gimac.des.pilotage.application.port.out.RenduPdfPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RenduPdfAdapterTest {
    static final RenduPdfPort.Document DOCUMENT = new RenduPdfPort.Document(
        "<h2>Données</h2>", "Demande de changement", "Accès en lecture", "CHG-1", "INTERNE", "fr", Instant.parse("2026-11-06T09:30:00Z"), List.of(new RenduPdfPort.Fait("État", "En cours"))
    );

    @Test
    void requeteAuGabaritCommun() {
        RenduPdfRestAdapter.Requete r = RenduPdfRestAdapter.requete(DOCUMENT);
        assertThat(r.html()).isEqualTo("<h2>Données</h2>");
        assertThat(r.piedDePage()).isNull();
        assertThat(r.orientation()).isEqualTo("PORTRAIT");
        assertThat(r.gabarit()).isEqualTo(new RenduPdfRestAdapter.Gabarit("Demande de changement", "Accès en lecture", "CHG-1", "INTERNE", "fr", "2026-11-06T09:30Z", DOCUMENT.faits()));
    }

    private MockRestServiceServer serveur;
    private RenduPdfRestAdapter adaptateur;

    @BeforeEach
    void init() {
        RestClient.Builder builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId("service-token")
            .clientId("pilotage-service").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).tokenUri("http://keycloak.test/token").build();
        OAuth2AccessToken acces = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-de-service", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(enregistrement, "service", acces));
        adaptateur = new RenduPdfRestAdapter(builder, new ServiceTokenInterceptor(manager), "http://document");
    }

    @Test
    void renduReelEtErreurs() {
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"format\":\"A4\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"gabarit\":{\"titre\":\"Demande de changement\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"edition\":\"2026-11-06T09:30Z\"")))
            .andRespond(withSuccess("%PDF-1.4\ncontenu", MediaType.APPLICATION_PDF));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("texte", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withServerError());
        assertThat(adaptateur.rendre(DOCUMENT)).isEqualTo("%PDF-1.4\ncontenu".getBytes());
        assertThatThrownBy(() -> adaptateur.rendre(DOCUMENT)).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.code()).isEqualTo("PIL_PDF_INDISPONIBLE"));
        assertThatThrownBy(() -> adaptateur.rendre(DOCUMENT)).isInstanceOf(PilotageException.class);
        serveur.verify();
    }

    @Test
    void reponsesVidesCourtesOuSansMarquePdf() {
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess());
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("x", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("%PDF", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("XPDFx", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("%XPDx", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("%PXDx", MediaType.APPLICATION_OCTET_STREAM));
        serveur.expect(requestTo("http://document/api/v1/interne/rendus/pdf")).andRespond(withSuccess("%PPXx", MediaType.APPLICATION_OCTET_STREAM));
        for (int i = 0; i < 7; i++) {
            assertThatThrownBy(() -> adaptateur.rendre(DOCUMENT)).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.code()).isEqualTo("PIL_PDF_INDISPONIBLE"));
        }
        serveur.verify();
    }

    @Test
    void etatIllegalDuClientHttp() throws Exception {
        ServiceTokenInterceptor jeton = mock(ServiceTokenInterceptor.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("panne")).when(jeton).intercept(any(), any(), any());
        RenduPdfRestAdapter a = new RenduPdfRestAdapter(RestClient.builder(), jeton, "http://document");
        assertThatThrownBy(() -> a.rendre(DOCUMENT)).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.code()).isEqualTo("PIL_PDF_INDISPONIBLE"));
    }

    @Test
    void substitutEstUnPdf() {
        assertThat(new String(new RenduPdfStubAdapter().rendre(DOCUMENT), 0, 4)).isEqualTo("%PDF");
    }
}
