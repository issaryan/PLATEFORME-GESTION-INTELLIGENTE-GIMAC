package cm.gimac.des.pilotage.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Portee;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.UniteMembres;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import java.time.Instant;
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

/**
 * Adaptateurs réels des processus d'unité (D-87) : membres d'une unité et chef (MS-IAM), profils d'un compte,
 * actions PROCESSUS_* de la matrice (absentes : AUCUNE), vérification FIDO2 (MS-IAM), avec le jeton de service.
 */
class AdaptateursProcessusTest {

    private static final String IAM = "http://iam.test";

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
    @DisplayName("Unité lue avant ses membres (libellé, état, chef), cache 60 s ; unité inconnue : vide ; sans membre : chef et liste vide")
    void membres() {
        IamOrganisationAdapter a = new IamOrganisationAdapter(builder, jeton, IAM, 60);
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/unites/POLE_RESEAU"))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(
                withSuccess(
                    "{\"code\":\"POLE_RESEAU\",\"libelle\":\"Pôle Réseau\",\"type\":\"POLE\",\"parent\":\"PROD_INFO\",\"active\":true,\"responsableId\":\"c1\"}",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/unites/POLE_RESEAU/membres?sousUnites=true&profil=ROLE_POLE_RESEAU"))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(
                withSuccess(
                    "[{\"id\":\"i1\",\"nomAffiche\":\"Ingrid\",\"fonction\":\"Ingénieure\",\"unite\":\"POLE_RESEAU\",\"responsableId\":\"c1\",\"actif\":true,\"profils\":[\"ROLE_POLE_RESEAU\"]}]",
                    MediaType.APPLICATION_JSON
                )
            );
        serveur.expect(requestTo(IAM + "/api/v1/interne/organisation/unites/FANTOME")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/unites/VIDE"))
            .andRespond(withSuccess("{\"code\":\"VIDE\",\"libelle\":\"Cellule vide\",\"active\":false,\"responsableId\":\"c9\"}", MediaType.APPLICATION_JSON));
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/unites/VIDE/membres?sousUnites=true"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        UniteMembres u = a.membresUnite("POLE_RESEAU", "ROLE_POLE_RESEAU").orElseThrow();
        assertThat(u.code()).isEqualTo("POLE_RESEAU");
        assertThat(u.libelle()).isEqualTo("Pôle Réseau");
        assertThat(u.active()).isTrue();
        assertThat(u.responsableId()).isEqualTo("c1");
        assertThat(u.profilConnu()).isTrue();
        assertThat(u.actifs()).extracting(m -> m.id()).containsExactly("i1");
        assertThat(u.membres().get(0).profils()).containsExactly("ROLE_POLE_RESEAU");
        // Cache : aucun nouvel appel.
        assertThat(a.membresUnite("POLE_RESEAU", "ROLE_POLE_RESEAU")).isPresent();
        // Unité inconnue : la route des membres n'est pas appelée.
        assertThat(a.membresUnite("FANTOME", null)).isEmpty();
        UniteMembres vide = a.membresUnite("VIDE", null).orElseThrow();
        assertThat(vide.responsableId()).isEqualTo("c9");
        assertThat(vide.active()).isFalse();
        assertThat(vide.membres()).isEmpty();
        assertThat(a.membresUnite(" ", null)).isEmpty();
        serveur.verify();
    }

    @Test
    @DisplayName("Unité : MS-IAM en erreur (5xx) : 503 PIL_ORGANISATION_INDISPONIBLE, rien n'est supposé ni mis en cache")
    void uniteIndisponible() {
        IamOrganisationAdapter a = new IamOrganisationAdapter(builder, jeton, IAM, 60);
        serveur.expect(requestTo(IAM + "/api/v1/interne/organisation/unites/POLE_RESEAU")).andRespond(withServerError());
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/organisation/unites/POLE_RESEAU"))
            .andRespond(withSuccess("{\"code\":\"POLE_RESEAU\",\"active\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/organisation/unites/POLE_RESEAU/membres?sousUnites=true")).andRespond(withServerError());
        assertThatThrownBy(() -> a.membresUnite("POLE_RESEAU", null)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_ORGANISATION_INDISPONIBLE")
        );
        assertThatThrownBy(() -> a.membresUnite("POLE_RESEAU", null)).isInstanceOfSatisfying(PilotageException.class, e ->
            assertThat(e.code()).isEqualTo("PIL_ORGANISATION_INDISPONIBLE")
        );
        serveur.verify();
    }

    @Test
    @DisplayName("Profils d'un compte et actions PROCESSUS_* de la matrice ; absentes ou inconnues : AUCUNE ; MS-IAM en erreur : 503")
    void profilsEtPouvoirs() {
        IamOrganisationAdapter a = new IamOrganisationAdapter(builder, jeton, IAM, 60);
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/collaborateurs/c1"))
            .andRespond(withSuccess("{\"id\":\"c1\",\"nomAffiche\":\"Brice\",\"profils\":[\"ROLE_RESPONSABLE_SERVICE\"],\"actif\":true}", MediaType.APPLICATION_JSON));
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/pouvoirs/c1"))
            .andRespond(withSuccess("{\"actions\":{\"PROCESSUS_DEFINIR\":\"UNITES\",\"ACTION_FUTURE\":\"UNITES\"},\"unitesDirigees\":[\"POLE_RESEAU\"]}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/collaborateurs/c2")).andRespond(withServerError());
        assertThat(a.profilsDe("c1")).containsExactly("ROLE_RESPONSABLE_SERVICE");
        assertThat(a.profilsDe(null)).isEmpty();
        var p = a.pouvoirs("c1");
        assertThat(p.portee(Pouvoir.PROCESSUS_DEFINIR)).isEqualTo(Portee.UNITES);
        assertThat(p.portee(Pouvoir.PROCESSUS_LANCER)).isEqualTo(Portee.AUCUNE);
        assertThatThrownBy(() -> a.profilsDe("c2")).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.code()).isEqualTo("PIL_ORGANISATION_INDISPONIBLE"));
        serveur.verify();
    }

    @Test
    @DisplayName("FIDO2 : vérification par MS-IAM (sujet, finalité SIGNATURE_PROCESSUS) ; refus 4xx : faux ; panne : 503")
    void reauthentification() {
        IamReauthentificationAdapter a = new IamReauthentificationAdapter(builder, jeton, IAM);
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/reauthentifications/verification"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andExpect(content().json("{\"assertion\":\"a1\",\"sujet\":\"c1\",\"finalite\":\"SIGNATURE_PROCESSUS\"}"))
            .andRespond(withSuccess("{\"valide\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/reauthentifications/verification")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        serveur.expect(requestTo(IAM + "/api/v1/interne/reauthentifications/verification")).andRespond(withSuccess("{\"valide\":false}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/reauthentifications/verification")).andRespond(withServerError());
        assertThat(a.verifier("a1", "c1", "SIGNATURE_PROCESSUS")).isTrue();
        assertThat(a.verifier("a2", "c1", "SIGNATURE_PROCESSUS")).isFalse();
        assertThat(a.verifier("a3", "c1", "SIGNATURE_PROCESSUS")).isFalse();
        assertThatThrownBy(() -> a.verifier("a4", "c1", "SIGNATURE_PROCESSUS")).isInstanceOfSatisfying(PilotageException.class, e -> assertThat(e.code()).isEqualTo("PIL_REAUTH_INDISPONIBLE"));
        assertThat(a.verifier(" ", "c1", "SIGNATURE_PROCESSUS")).isFalse();
        serveur.verify();
        ReauthentificationStubAdapter s = new ReauthentificationStubAdapter();
        assertThat(s.verifier("x", "c1", "SIGNATURE_PROCESSUS")).isTrue();
        assertThat(s.verifier(ReauthentificationStubAdapter.ASSERTION_REFUSEE, "c1", "SIGNATURE_PROCESSUS")).isFalse();
        assertThat(s.verifications()).hasSize(2);
    }

    @Test
    @DisplayName("Organisation simulée : profils, membres par profil (sous-unités), profil inconnu, matrice des responsables")
    void bouchon() {
        OrganisationStubAdapter o = new OrganisationStubAdapter(true);
        assertThat(o.profilsDe(OrganisationStubAdapter.INGENIEUR_RESEAU)).containsExactly("ROLE_POLE_RESEAU");
        assertThat(o.membresUnite("PROD_INFO", "ROLE_POLE_RESEAU").orElseThrow().actifs()).hasSize(2);
        assertThat(o.membresUnite("POLE_RESEAU", null).orElseThrow().responsableId()).isEqualTo(OrganisationStubAdapter.CHEF_RESEAU);
        assertThat(o.membresUnite("POLE_RESEAU", "ROLE_INEXISTANT").orElseThrow().profilConnu()).isFalse();
        assertThat(o.membresUnite("FANTOME", null)).isEmpty();
        assertThat(o.pouvoirs(OrganisationStubAdapter.CHEF_RESEAU).portee(Pouvoir.PROCESSUS_LANCER)).isEqualTo(Portee.UNITES);
        assertThat(new OrganisationStubAdapter().membresUnite("POLE_RESEAU", "ROLE_POLE_RESEAU").orElseThrow().membres()).isEmpty();
    }
}
