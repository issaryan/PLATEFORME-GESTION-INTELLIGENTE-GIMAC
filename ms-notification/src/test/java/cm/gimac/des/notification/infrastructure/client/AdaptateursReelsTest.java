package cm.gimac.des.notification.infrastructure.client;

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

import cm.gimac.des.notification.application.port.out.ConversationsPort;
import cm.gimac.des.notification.domain.exception.NotificationException;
import java.time.Instant;
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

/** Adaptateurs réels vers MS-IAM et MS-COLLABORATION avec le jeton de service, cache et pannes. */
class AdaptateursReelsTest {

    private static final String IAM = "http://iam.test";
    private static final String COL = "http://collaboration.test";

    private RestClient.Builder builder;
    private MockRestServiceServer serveur;
    private ServiceTokenInterceptor jeton;

    @BeforeEach
    void preparer() {
        builder = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(builder).build();
        OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
        ClientRegistration enregistrement = ClientRegistration.withRegistrationId("service-token")
            .clientId("notification-service")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak.test/token")
            .build();
        OAuth2AccessToken acces = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "jeton-de-service", Instant.now(), Instant.now().plusSeconds(60));
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(enregistrement, "service", acces));
        jeton = new ServiceTokenInterceptor(manager);
    }

    @Test
    @DisplayName("Annuaire : nom lu avec le jeton de service puis servi par le cache ; inconnu vide ; panne en 503")
    void annuaire() {
        serveur
            .expect(requestTo(IAM + "/api/v1/interne/collaborateurs/c1"))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(withSuccess("{\"id\":\"c1\",\"nomAffiche\":\"Carine Ebongue Ndjock\",\"profils\":[],\"actif\":true}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(IAM + "/api/v1/interne/collaborateurs/inconnu")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(IAM + "/api/v1/interne/collaborateurs/panne")).andRespond(withServerError());
        IamAnnuaireAdapter a = new IamAnnuaireAdapter(builder, jeton, IAM, 300);
        assertThat(a.nomAffiche("c1")).contains("Carine Ebongue Ndjock");
        assertThat(a.nomAffiche("c1")).contains("Carine Ebongue Ndjock");
        assertThat(a.nomAffiche("inconnu")).isEmpty();
        assertThatThrownBy(() -> a.nomAffiche("panne")).isInstanceOf(NotificationException.class).extracting("code").isEqualTo("NOT_ANNUAIRE_INDISPONIBLE");
        serveur.verify();
    }

    @Test
    @DisplayName("Conversations : { id, type, nom, membres } de la route interne, cache ; inconnue vide ; panne en 503")
    void conversations() {
        serveur
            .expect(requestTo(COL + "/api/v1/interne/conversations/c-1"))
            .andExpect(header("Authorization", "Bearer jeton-de-service"))
            .andRespond(withSuccess("{\"id\":\"c-1\",\"type\":\"GROUPE\",\"nom\":\"Groupe Cadrage L-12\",\"membres\":[\"a\",\"b\"]}", MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(COL + "/api/v1/interne/conversations/c-2")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        serveur.expect(requestTo(COL + "/api/v1/interne/conversations/c-3")).andRespond(withServerError());
        CollaborationConversationsAdapter a = new CollaborationConversationsAdapter(builder, jeton, COL, 60);
        ConversationsPort.Conversation c = a.conversation("c-1").orElseThrow();
        assertThat(c.estGroupe()).isTrue();
        assertThat(c.nom()).isEqualTo("Groupe Cadrage L-12");
        assertThat(c.membres()).containsExactly("a", "b");
        assertThat(a.conversation("c-1")).isPresent();
        assertThat(a.conversation("c-2")).isEmpty();
        assertThatThrownBy(() -> a.conversation("c-3")).isInstanceOf(NotificationException.class).extracting("code").isEqualTo("NOT_COLLABORATION_INDISPONIBLE");
        serveur.verify();
    }

    @Test
    @DisplayName("Adaptateurs de substitution : noms connus, panne simulée, groupes déclarés")
    void substitution() {
        AnnuaireStubAdapter annuaire = new AnnuaireStubAdapter();
        assertThat(annuaire.nomAffiche(AnnuaireStubAdapter.CHEF_DES)).contains("Paul Mbarga Atangana");
        annuaire.rendreIndisponible(true);
        assertThatThrownBy(() -> annuaire.nomAffiche(AnnuaireStubAdapter.CHEF_DES)).isInstanceOf(NotificationException.class);
        annuaire.reinitialiser();
        assertThat(annuaire.nomAffiche("inconnu")).isEmpty();
        ConversationsStubAdapter conversations = new ConversationsStubAdapter();
        assertThat(conversations.conversation("c")).isEmpty();
        conversations.groupe("c", "Comité", java.util.List.of("a"));
        assertThat(conversations.conversation("c")).get().extracting(ConversationsPort.Conversation::nom).isEqualTo("Comité");
    }
}
