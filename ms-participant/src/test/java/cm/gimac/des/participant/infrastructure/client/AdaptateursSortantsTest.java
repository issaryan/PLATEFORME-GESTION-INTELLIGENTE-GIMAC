package cm.gimac.des.participant.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.participant.application.port.out.DocumentRegistry.Verification;
import cm.gimac.des.participant.domain.ParticipantException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs sortants (PI §0.8) : substituts, adaptateurs réels, jeton de service, pannes sans valeur par défaut. */
class AdaptateursSortantsTest {

    static final UUID DOC = UUID.randomUUID();
    static final String SHA = "b".repeat(64);

    RestClient.Builder builder;
    MockRestServiceServer serveur;
    ServiceTokenInterceptor jeton;

    @BeforeEach
    void init() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration reg = ClientRegistration.withRegistrationId("service-token")
            .clientId("participant-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://idp/token")
            .build();
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-service", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(reg, "service", token));
        jeton = new ServiceTokenInterceptor(manager);
    }

    @Test
    @DisplayName("Substituts : pièce toujours conforme ; réauthentification acceptée si l'assertion est non vide")
    void substituts() {
        assertThat(new StubDocumentRegistry().verifier(DOC, SHA)).isEqualTo(Verification.CONFORME);
        StubStrongReauthVerifier stub = new StubStrongReauthVerifier();
        assertThat(stub.verifier("x", UUID.randomUUID(), "F")).isTrue();
        assertThat(stub.verifier(" ", UUID.randomUUID(), "F")).isFalse();
        assertThat(stub.verifier(null, UUID.randomUUID(), "F")).isFalse();
    }

    @Test
    @DisplayName("Registre documentaire réel : empreinte identique, divergente, absente ou inconnue")
    void registreDocumentaire() {
        HttpDocumentRegistry registre = new HttpDocumentRegistry(builder, jeton, "http://document");
        String uri = "http://document/api/v1/interne/documents/" + DOC + "/empreinte";
        serveur.expect(requestTo(uri)).andExpect(header("Authorization", "Bearer jeton-service")).andRespond(withSuccess("{\"sha256\":\"" + SHA.toUpperCase() + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{\"sha256\":\"" + "c".repeat(64) + "\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withResourceNotFound());
        assertThat(registre.verifier(DOC, SHA)).isEqualTo(Verification.CONFORME);
        assertThat(registre.verifier(DOC, SHA)).isEqualTo(Verification.DIVERGENTE);
        assertThat(registre.verifier(DOC, SHA)).isEqualTo(Verification.INCONNUE);
        assertThat(registre.verifier(DOC, SHA)).isEqualTo(Verification.INCONNUE);
        serveur.verify();
    }

    @Test
    @DisplayName("Registre documentaire indisponible ou refusant l'appel : 503")
    void registreIndisponible() {
        HttpDocumentRegistry registre = new HttpDocumentRegistry(builder, jeton, "http://document");
        serveur.expect(requestTo("http://document/api/v1/interne/documents/" + DOC + "/empreinte")).andRespond(withServerError());
        serveur.expect(requestTo("http://document/api/v1/interne/documents/" + DOC + "/empreinte")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> registre.verifier(DOC, SHA))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.status()).isEqualTo(503));
        }
    }

    @Test
    @DisplayName("Réauthentification réelle : verdict de MS-IAM, refus 4xx, panne 503, assertion vide sans appel")
    void reauthentification() {
        IamStrongReauthVerifier iam = new IamStrongReauthVerifier(builder, jeton, "http://iam");
        UUID sujet = UUID.randomUUID();
        String uri = "http://iam/api/v1/interne/reauthentifications/verification";
        serveur
            .expect(requestTo(uri))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.assertion").value("A"))
            .andExpect(jsonPath("$.finalite").value("PAR_TRANSITION_APPROVE"))
            .andRespond(withSuccess("{\"valide\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withSuccess("{\"valide\":false}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(uri)).andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));
        serveur.expect(requestTo(uri)).andRespond(withServerError());
        assertThat(iam.verifier("A", sujet, "PAR_TRANSITION_APPROVE")).isTrue();
        assertThat(iam.verifier("A", sujet, "PAR_TRANSITION_APPROVE")).isFalse();
        assertThat(iam.verifier("A", sujet, "PAR_TRANSITION_APPROVE")).isFalse();
        assertThatThrownBy(() -> iam.verifier("A", sujet, "PAR_TRANSITION_APPROVE"))
            .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_IAM_UNAVAILABLE"));
        assertThat(iam.verifier("", sujet, "F")).isFalse();
        assertThat(iam.verifier(null, sujet, "F")).isFalse();
        serveur.verify();
    }

    @Test
    @DisplayName("Jeton de service indisponible : l'appel n'est pas émis")
    void jetonIndisponible() {
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        HttpDocumentRegistry registre = new HttpDocumentRegistry(builder, new ServiceTokenInterceptor(manager), "http://document");
        assertThatThrownBy(() -> registre.verifier(DOC, SHA)).isInstanceOf(IllegalStateException.class);
    }
}
