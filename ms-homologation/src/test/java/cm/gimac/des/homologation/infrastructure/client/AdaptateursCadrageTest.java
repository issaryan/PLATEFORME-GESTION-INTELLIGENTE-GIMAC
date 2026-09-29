package cm.gimac.des.homologation.infrastructure.client;

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

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.cadrage.CollaboratorView;
import cm.gimac.des.homologation.domain.cadrage.InstitutionView;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

/** Adaptateurs du cadrage : registre des institutions et annuaire des collaborateurs, réels et de substitution. */
class AdaptateursCadrageTest {

    static final UUID ID = UUID.randomUUID();

    static ServiceTokenInterceptor jeton() {
        OAuth2AuthorizedClientManager m = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration r = ClientRegistration.withRegistrationId("service-token").clientId("homologation-service").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).tokenUri("http://idp/token").build();
        when(m.authorize(any())).thenReturn(new OAuth2AuthorizedClient(r, "service", new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton", Instant.now(), Instant.now().plusSeconds(60))));
        return new ServiceTokenInterceptor(m);
    }

    @Test
    @DisplayName("Registre réel : lecture par l'interface interne avec le jeton de service ; 404 : inconnue ; 5xx : indisponible")
    void participantReel() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        ParticipantRestAdapter a = new ParticipantRestAdapter(b, jeton(), "http://participant");
        String url = "http://participant/api/v1/interne/institutions/" + ID;
        serveur
            .expect(requestTo(url))
            .andExpect(header("Authorization", "Bearer jeton"))
            .andRespond(withSuccess("{\"id\":\"" + ID + "\",\"nom\":\"Banque B\",\"statut\":\"ADHERENT\",\"categorie\":\"BANQUE\",\"servicesSouscrits\":[{\"code\":\"CARTE_INTERBANCAIRE\",\"depuis\":\"2026-09-01\"}]}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        InstitutionView v = a.getInstitution(ID);
        assertThat(v.statut()).isEqualTo("ADHERENT");
        assertThat(v.servicesSouscrits()).extracting(InstitutionView.ServiceSouscrit::code).containsExactly("CARTE_INTERBANCAIRE");
        assertThatThrownBy(() -> a.getInstitution(ID)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_LIAISON_INSTITUTION_UNKNOWN"));
        assertThatThrownBy(() -> a.getInstitution(ID)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.status()).isEqualTo(503));
        serveur.verify();
    }

    @Test
    @DisplayName("Annuaire réel : collaborateur, absent, par profil ; panne : indisponible")
    void collaborateursReels() {
        RestClient.Builder b = RestClient.builder();
        MockRestServiceServer serveur = MockRestServiceServer.bindTo(b).build();
        IamCollaboratorAdapter a = new IamCollaboratorAdapter(b, jeton(), "http://iam");
        String json = "{\"id\":\"" + ID + "\",\"nomAffiche\":\"Aminata Ngono\",\"profils\":[\"ROLE_CONDUCTEUR_PROJET\"],\"institutionId\":null,\"actif\":true}";
        String url = "http://iam/api/v1/interne/collaborateurs/" + ID;
        serveur.expect(requestTo(url)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo("http://iam/api/v1/interne/collaborateurs?profil=ROLE_CONDUCTEUR_PROJET")).andRespond(withSuccess("[" + json + "]", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(url)).andRespond(withServerError());
        serveur.expect(requestTo("http://iam/api/v1/interne/collaborateurs?profil=X")).andRespond(withServerError());
        assertThat(a.findCollaborator(ID)).map(CollaboratorView::nomAffiche).contains("Aminata Ngono");
        assertThat(a.findCollaborator(ID)).isEmpty();
        assertThat(a.parProfil("ROLE_CONDUCTEUR_PROJET")).hasSize(1);
        assertThatThrownBy(() -> a.findCollaborator(ID)).isInstanceOfSatisfying(HomologationException.class, e -> assertThat(e.code()).isEqualTo("HOM_IAM_UNAVAILABLE"));
        assertThatThrownBy(() -> a.parProfil("X")).isInstanceOf(HomologationException.class);
        serveur.verify();
    }

    @Test
    @DisplayName("Substituts : jeux de démonstration, enregistrement, inconnue refusée, filtre par profil")
    void substituts() {
        ParticipantStubAdapter p = new ParticipantStubAdapter();
        assertThat(p.getInstitution(ParticipantStubAdapter.EP_ADHERENT).statut()).isEqualTo("ADHERENT");
        p.enregistrer(new InstitutionView(ID, "X", "CANDIDAT", "BANQUE", List.of()));
        assertThat(p.getInstitution(ID).statut()).isEqualTo("CANDIDAT");
        assertThatThrownBy(() -> p.getInstitution(UUID.randomUUID())).isInstanceOf(HomologationException.class);
        CollaboratorStubAdapter c = new CollaboratorStubAdapter();
        c.enregistrer(new CollaboratorView(ID, "Inactif", Set.of("ROLE_CONDUCTEUR_PROJET"), null, false));
        assertThat(c.parProfil("ROLE_CONDUCTEUR_PROJET")).extracting(CollaboratorView::nomAffiche).containsExactly("Aminata Ngono", "Paul Essomba");
        assertThat(c.findCollaborator(CollaboratorStubAdapter.CONDUCTEUR_1)).isPresent();
    }
}
