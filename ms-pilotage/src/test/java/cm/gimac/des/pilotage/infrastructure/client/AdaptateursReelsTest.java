package cm.gimac.des.pilotage.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adaptateurs réels vers MS-IAM (représentations du contrat §1.3) et MS-DOCUMENT, avec le jeton de service. */
class AdaptateursReelsTest {

    private static final String IAM = "http://iam.test";
    private static final String DOC = "http://document.test";

    private RestClient.Builder builder;
    private MockRestServiceServer serveur;
    private ServiceTokenInterceptor jeton;

    @BeforeEach
    void preparer() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId("service-token")
            .clientId("pilotage-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak.test/token")
            .build();
        OAuth2AccessToken acces = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-de-service", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(enregistrement, "service", acces));
        jeton = new ServiceTokenInterceptor(manager);
    }

    @Test
    @DisplayName("Organisation : rattachement, équipe et collaborateur lus avec le jeton de service, puis servis par le cache")
    void organisation() {
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/a1"))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(
                withSuccess(
                    "{\"id\":\"a1\",\"nomAffiche\":\"Serge\",\"unite\":{\"code\":\"POLE_RESEAU\",\"libelle\":\"Pôle réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\",\"active\":true}," +
                    "\"chemin\":[],\"responsable\":{\"id\":\"r1\",\"nomAffiche\":\"Aminata\",\"fonction\":\"Cheffe\"},\"chaine\":[\"r1\",\"r2\"],\"equipeDirecte\":0}",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/r1/equipe"))
            .andRespond(
                withSuccess(
                    "[{\"id\":\"a1\",\"nomAffiche\":\"Serge\",\"fonction\":\"Ingénieur\",\"unite\":\"POLE_RESEAU\",\"responsableId\":\"r1\",\"niveau\":1,\"actif\":true}]",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/collaborateurs/c1"))
            .andRespond(withSuccess("{\"id\":\"c1\",\"nomAffiche\":\"Jeanne\",\"profils\":[],\"institutionId\":\"i1\",\"actif\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/organisation/inconnu")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(IAM + "/api/v1/interne/organisation/x/equipe")).andRespond(withServerError());

        IamOrganisationAdapter iam = new IamOrganisationAdapter(builder, jeton, IAM, 60);
        OrganisationPort.Rattachement r = iam.rattachement("a1").orElseThrow();
        assertThat(r.unite()).isEqualTo("POLE_RESEAU");
        assertThat(r.responsable().id()).isEqualTo("r1");
        assertThat(r.chaine()).containsExactly("r1", "r2");
        assertThat(iam.equipe("r1")).singleElement().satisfies(m -> {
            assertThat(m.id()).isEqualTo("a1");
            assertThat(m.niveau()).isEqualTo(1);
            assertThat(m.actif()).isTrue();
        });
        assertThat(iam.collaborateur("c1").orElseThrow().interne()).isFalse();
        assertThat(iam.rattachement("inconnu")).isEmpty();
        assertThatThrownBy(() -> iam.equipe("x")).isInstanceOfSatisfying(PilotageException.class, e -> {
            assertThat(e.status()).isEqualTo(503);
            assertThat(e.code()).isEqualTo("PIL_ORGANISATION_INDISPONIBLE");
        });
        // Cache de 60 s : aucune nouvelle requête.
        assertThat(iam.rattachement("a1")).isPresent();
        assertThat(iam.equipe("r1")).hasSize(1);
        serveur.verify();
    }

    @Test
    @DisplayName("Documents : métadonnées d'un ticket (404 : vide) et contenu relayé en flux")
    void documents() throws Exception {
        UUID id = UUID.randomUUID();
        serveur
            .expect(requestTo(DOC + "/api/v1/interne/documents/" + id))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(
                withSuccess(
                    "{\"id\":\"" + id + "\",\"nomFichier\":\"rapport.pdf\",\"taille\":4,\"typeMime\":\"application/pdf\",\"sha256\":\"ab\",\"finalite\":\"RAPPORT_ACTIVITE\",\"reference\":\"r\",\"deposant\":\"a1\"}",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur.expect(requestTo(DOC + "/api/v1/interne/documents/" + id + "/contenu")).andRespond(withSuccess("%PDF".getBytes(StandardCharsets.US_ASCII), MediaType.APPLICATION_PDF));
        UUID inconnu = UUID.randomUUID();
        serveur.expect(requestTo(DOC + "/api/v1/interne/documents/" + inconnu)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        DocumentRestAdapter doc = new DocumentRestAdapter(builder, jeton, DOC);
        DocumentPort.Metadonnees m = doc.metadonnees(id).orElseThrow();
        assertThat(m.finalite()).isEqualTo("RAPPORT_ACTIVITE");
        assertThat(m.deposant()).isEqualTo("a1");
        assertThat(m.taille()).isEqualTo(4);
        try (var flux = doc.lire(id).flux()) {
            assertThat(new String(flux.readAllBytes(), StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }
        assertThat(doc.metadonnees(inconnu)).isEmpty();
        serveur.verify();
    }
}
