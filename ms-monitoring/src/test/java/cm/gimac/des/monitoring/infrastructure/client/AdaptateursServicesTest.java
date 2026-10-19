package cm.gimac.des.monitoring.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.monitoring.application.port.out.AnnuairePort;
import cm.gimac.des.monitoring.application.port.out.DisponibilitesPort;
import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Adaptateurs réels vers MS-IAM et MS-DOCUMENT (D-114, §2, §6, §7.4) : chemins, jeton de service et corrélation,
 * filtrage des réponses, refus traduits en codes MON_* stables, service injoignable en 503.
 */
class AdaptateursServicesTest {

    private RestClient.Builder builder;
    private MockRestServiceServer serveur;
    private ServiceTokenInterceptor jeton;

    @BeforeEach
    void preparer() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId(ServiceTokenInterceptor.REGISTRATION)
            .clientId("monitoring-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://iam.test/jeton")
            .build();
        OAuth2AccessToken acces = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-de-test", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(enregistrement, "service", acces));
        jeton = new ServiceTokenInterceptor(manager);
    }

    @Test
    void annuairePorteursFiltresTriesEtNomsEnCache() {
        AnnuaireIamAdapter a = new AnnuaireIamAdapter(builder, jeton, "http://iam.test");
        serveur
            .expect(requestTo(Matchers.startsWith("http://iam.test/api/v1/interne/collaborateurs")))
            .andExpect(queryParam("permission", "MON_SHIFT_OPERATOR"))
            .andExpect(header("Authorization", "Bearer jeton-de-test"))
            .andRespond(
                withSuccess(
                    "[{\"id\":\"u2\",\"nomAffiche\":\"Zoé\",\"actif\":true},{\"id\":\"u1\",\"nomAffiche\":\"Amina\"}," +
                    "{\"id\":\"u3\",\"nomAffiche\":\"Inactif\",\"actif\":false},{\"id\":\"u4\",\"nomAffiche\":\"Correspondant\",\"institutionId\":\"i1\"}," +
                    "{\"id\":\"u5\"},{\"nomAffiche\":\"Sans identifiant\"}]",
                    MediaType.APPLICATION_JSON
                )
            );
        List<AnnuairePort.Personne> l = a.porteurs("MON_SHIFT_OPERATOR");
        assertThat(l).extracting(AnnuairePort.Personne::id).containsExactly("u1", "u2", "u5");
        assertThat(l.get(2).nom()).isEqualTo("u5");

        serveur.reset();
        serveur.expect(requestTo("http://iam.test/api/v1/interne/collaborateurs/u1")).andRespond(withSuccess("{\"id\":\"u1\",\"nomAffiche\":\"Amina\"}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo("http://iam.test/api/v1/interne/collaborateurs/u9")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(List.of("u1", "u9"));
        ids.add(null);
        assertThat(a.noms(ids)).isEqualTo(Map.of("u1", "Amina"));
        serveur.verify();
        // Deuxième lecture : servie par le cache, aucun appel.
        serveur.reset();
        assertThat(a.noms(List.of("u1"))).isEqualTo(Map.of("u1", "Amina"));
        serveur.verify();
    }

    @Test
    void annuaireInjoignable() {
        AnnuaireIamAdapter a = new AnnuaireIamAdapter(builder, jeton, "http://iam.test");
        serveur.expect(requestTo(Matchers.startsWith("http://iam.test/api/v1/interne/collaborateurs"))).andRespond(withServerError());
        assertThatThrownBy(() -> a.porteurs("MON_SHIFT_LEAD")).isInstanceOf(MonitoringException.class).hasFieldOrPropertyWithValue("code", "MON_ANNUAIRE_INDISPONIBLE");
        serveur.reset();
        serveur.expect(requestTo("http://iam.test/api/v1/interne/collaborateurs/u1")).andRespond(withServerError());
        assertThat(a.noms(List.of("u1"))).isEmpty();
    }

    @Test
    void disponibilitesValideesEtIncompletesEcartees() {
        DisponibilitesIamAdapter a = new DisponibilitesIamAdapter(builder, jeton, "http://iam.test");
        UUID id = UUID.randomUUID();
        serveur
            .expect(requestTo(Matchers.startsWith("http://iam.test/api/v1/interne/disponibilites")))
            .andExpect(queryParam("du", "2026-10-26T05:00:00Z"))
            .andExpect(queryParam("au", "2026-11-02T05:00:00Z"))
            .andRespond(
                withSuccess(
                    "[{\"id\":\"" + id + "\",\"compteId\":\"u1\",\"debut\":\"2026-10-27T00:00:00Z\",\"fin\":\"2026-10-28T00:00:00Z\",\"annulee\":true}," +
                    "{\"id\":\"" + UUID.randomUUID() + "\",\"compteId\":\"u2\",\"debut\":\"2026-10-27T00:00:00Z\"}]",
                    MediaType.APPLICATION_JSON
                )
            );
        List<DisponibilitesPort.Absence> l = a.absencesValidees(Instant.parse("2026-10-26T05:00:00Z"), Instant.parse("2026-11-02T05:00:00Z"));
        assertThat(l).hasSize(1);
        assertThat(l.get(0).id()).isEqualTo(id);
        assertThat(l.get(0).annulee()).isTrue();
        serveur.reset();
        serveur.expect(requestTo(Matchers.startsWith("http://iam.test/api/v1/interne/disponibilites"))).andRespond(withServerError());
        assertThatThrownBy(() -> a.absencesValidees(Instant.now(), Instant.now()))
            .isInstanceOf(MonitoringException.class)
            .hasFieldOrPropertyWithValue("code", "MON_DISPONIBILITES_INDISPONIBLES");
    }

    @Test
    void reauthentificationVerifieeRefuseeOuIndisponible() {
        IamReauthentificationAdapter a = new IamReauthentificationAdapter(builder, jeton, "http://iam.test");
        assertThat(a.verifier(null, "u1", "SIGNATURE_RELEVE")).isFalse();
        assertThat(a.verifier("a", " ", "SIGNATURE_RELEVE")).isFalse();
        serveur
            .expect(requestTo("http://iam.test/api/v1/interne/reauthentifications/verification"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"assertion\":\"a\",\"sujet\":\"u1\",\"finalite\":\"SIGNATURE_RELEVE\"}"))
            .andRespond(withSuccess("{\"valide\":true}", MediaType.APPLICATION_JSON));
        assertThat(a.verifier("a", "u1", "SIGNATURE_RELEVE")).isTrue();
        serveur.reset();
        serveur.expect(requestTo("http://iam.test/api/v1/interne/reauthentifications/verification")).andRespond(withSuccess("{\"valide\":false}", MediaType.APPLICATION_JSON));
        assertThat(a.verifier("a", "u1", "PUBLICATION_GRILLE")).isFalse();
        serveur.reset();
        serveur.expect(requestTo("http://iam.test/api/v1/interne/reauthentifications/verification")).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThat(a.verifier("a", "u1", "SIGNATURE_RELEVE")).isFalse();
        serveur.reset();
        serveur.expect(requestTo("http://iam.test/api/v1/interne/reauthentifications/verification")).andRespond(withServerError());
        assertThatThrownBy(() -> a.verifier("a", "u1", "SIGNATURE_RELEVE")).isInstanceOf(MonitoringException.class).hasFieldOrPropertyWithValue("code", "MON_REAUTH_INDISPONIBLE");
    }

    @Test
    void depotMultipartEtRefus() {
        DepotDocumentRestAdapter a = new DepotDocumentRestAdapter(builder, jeton, "http://document.test");
        serveur
            .expect(requestTo("http://document.test/api/v1/interne/documents"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", Matchers.startsWith("multipart/form-data")))
            .andExpect(content().string(Matchers.containsString("RAPPORT_QUART")))
            .andRespond(withSuccess("{\"id\":\"doc-1\"}", MediaType.APPLICATION_JSON));
        assertThat(a.deposer("{}".getBytes(), "RQ-2026-1019-M.json", "application/json", "e")).isEqualTo("doc-1");
        serveur.reset();
        serveur.expect(requestTo("http://document.test/api/v1/interne/documents")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> a.deposer(new byte[0], "x.json", "application/json", "e")).hasFieldOrPropertyWithValue("code", "MON_DEPOT_INDISPONIBLE");
        serveur.reset();
        serveur.expect(requestTo("http://document.test/api/v1/interne/documents")).andRespond(withServerError());
        assertThatThrownBy(() -> a.deposer(new byte[0], "x.json", "application/json", "e")).hasFieldOrPropertyWithValue("code", "MON_DEPOT_INDISPONIBLE");
    }

    @Test
    void sansJetonDeServiceAucunAppel() {
        OAuth2AuthorizedClientManager vide = mock(OAuth2AuthorizedClientManager.class);
        AnnuaireIamAdapter a = new AnnuaireIamAdapter(builder, new ServiceTokenInterceptor(vide), "http://iam.test");
        assertThatThrownBy(() -> a.porteurs("MON_SHIFT_OPERATOR")).hasFieldOrPropertyWithValue("code", "MON_ANNUAIRE_INDISPONIBLE");
    }
}
