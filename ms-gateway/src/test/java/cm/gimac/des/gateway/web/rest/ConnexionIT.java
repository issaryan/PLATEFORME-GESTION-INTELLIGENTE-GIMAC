package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import cm.gimac.des.gateway.GatewayApp;
import cm.gimac.des.gateway.config.AsyncSyncConfiguration;
import cm.gimac.des.gateway.config.DatabaseTestcontainer;
import cm.gimac.des.gateway.test.util.KeycloakSimule;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Intégration de la connexion sur la plateforme (D-65) dans le contexte complet de la passerelle :
 * enregistrements OAuth2 construits par Spring Boot depuis la découverte OIDC d'un Keycloak simulé,
 * route /bff/{couche}/iam/moi (OpaAuthorize, TokenRelay) vers un MS-IAM simulé, OPA simulé. Vérifie
 * que la session ouverte par l'octroi direct fonctionne comme une connexion OIDC : relais du jeton,
 * rafraîchissement par le client de la session, déconnexion avec révocation, témoin Secure.
 */
@SpringBootTest(classes = { GatewayApp.class, AsyncSyncConfiguration.class, DatabaseTestcontainer.class })
@org.junit.jupiter.api.Timeout(120)
class ConnexionIT {

    static final KeycloakSimule KEYCLOAK = new KeycloakSimule().dureeAcces(30);
    static final ServeurHttpLocal IAM;
    static final ServeurHttpLocal OPA;

    static {
        try {
            IAM = new ServeurHttpLocal();
            OPA = new ServeurHttpLocal().json("/v1/data/gimac/authz/allow", 200, "{\"result\":true}");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry r) {
        String reg = "spring.security.oauth2.client.registration.";
        r.add("spring.security.oauth2.client.provider.oidc.issuer-uri", KEYCLOAK::emetteur);
        r.add(reg + "oidc.client-id", () -> "web_app");
        r.add(reg + "oidc.client-secret", () -> "secret-web");
        r.add(reg + "oidc.scope", () -> "openid,profile");
        r.add(reg + "connexion.provider", () -> "oidc");
        r.add(reg + "connexion.client-id", () -> "gimac-connexion");
        r.add(reg + "connexion.client-secret", () -> "secret-connexion");
        r.add(reg + "connexion.scope", () -> "openid,profile");
        r.add(reg + "gateway-service.provider", () -> "oidc");
        r.add(reg + "gateway-service.client-id", () -> "gateway-service");
        r.add(reg + "gateway-service.client-secret", () -> "secret-service");
        r.add(reg + "gateway-service.authorization-grant-type", () -> "client_credentials");
        r.add("jhipster.security.oauth2.audience[0]", () -> "account");
        r.add("gimac.connexion.iam-url", IAM::url);
        r.add("gimac.opa.url", OPA::url);
        r.add("gimac.session.balayage", () -> "PT1H");
        r.add("server.reactive.session.cookie.secure", () -> "true");
        r.add("server.reactive.session.cookie.same-site", () -> "lax");
        String route = "spring.cloud.gateway.server.webflux.routes[0].";
        r.add("spring.cloud.gateway.server.webflux.default-filters[0]", () -> "TokenRelay");
        r.add(route + "id", () -> "iam-libre-service");
        r.add(route + "uri", IAM::url);
        r.add(route + "predicates[0]", () -> "Path=/bff/{couche}/iam/moi,/bff/{couche}/iam/moi/**");
        r.add(route + "filters[0]", () -> "OpaAuthorize");
        r.add(route + "filters[1]", () -> "FermetureAutresSessions");
        r.add(route + "filters[2]", () -> "RewritePath=/bff/(?<couche>des|portail|console)/iam/(?<p>.*), /api/v1/$\\{p}");
        String avatars = "spring.cloud.gateway.server.webflux.routes[1].";
        r.add(avatars + "id", () -> "iam-avatars");
        r.add(avatars + "uri", IAM::url);
        r.add(avatars + "predicates[0]", () -> "Path=/bff/des/iam/avatars/**,/bff/console/iam/avatars/**");
        r.add(avatars + "filters[0]", () -> "OpaAuthorize");
        r.add(avatars + "filters[1]", () -> "RewritePath=/bff/(?<couche>des|console)/iam/avatars/(?<p>.*), /api/v1/avatars/$\\{p}");
    }

    @AfterAll
    static void arreter() {
        KEYCLOAK.close();
        IAM.close();
        OPA.close();
    }

    @Autowired
    ApplicationContext contexte;

    WebTestClient client;

    @BeforeEach
    void preparer() {
        client = WebTestClient.bindToApplicationContext(contexte).apply(springSecurity()).configureClient().build();
        KEYCLOAK.reinitialiser();
        KEYCLOAK.dureeAcces(30);
        KEYCLOAK.compte("agent.sna", KeycloakSimule.interne("u-sna", "ROLE_SNA"));
        KEYCLOAK.compte("nouveau", KeycloakSimule.interne("u-nouveau", "ROLE_SNA"));
        IAM.json("/api/v1/interne/comptes/u-sna/etat-connexion", 200, "{\"actif\":true,\"secondFacteur\":true,\"actionsRequises\":[]}");
        IAM.json("/api/v1/interne/comptes/u-nouveau/etat-connexion", 200, "{\"actif\":true,\"secondFacteur\":false,\"actionsRequises\":[]}");
        IAM.json("/api/v1/moi", 200, "{\"sujet\":\"x\"}");
        IAM.recues().clear();
    }

    String connecter(String identifiant, String code, String etat) {
        String corps =
            "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"Mot-de-passe-1\"" + (code == null ? "" : ",\"code\":\"" + code + "\"") +
            ",\"couche\":\"des\"}";
        var r = client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(corps)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.etat")
            .isEqualTo(etat)
            .returnResult();
        ResponseCookie temoin = r.getResponseCookies().getFirst("SESSION");
        assertThat(temoin).isNotNull();
        assertThat(temoin.isHttpOnly()).isTrue();
        assertThat(temoin.isSecure()).isTrue();
        assertThat(temoin.getSameSite()).isEqualToIgnoringCase("Lax");
        return temoin.getValue();
    }

    @Test
    @DisplayName("Session complète : relais du jeton (TokenRelay) rafraîchi par web_app, OPA sans restriction, déconnexion révoquée")
    void sessionComplete() {
        String session = connecter("agent.sna", "123456", "CONNECTE");
        client.get().uri("/api/session").cookie("SESSION", session).exchange().expectStatus().isOk().expectBody().jsonPath("$.acr").isEqualTo("2");
        client.get().uri("/bff/des/iam/moi").cookie("SESSION", session).exchange().expectStatus().isOk();
        var relayee = IAM.recues().stream().filter(q -> q.chemin().equals("/api/v1/moi")).findFirst().orElseThrow();
        assertThat(relayee.autorisation()).startsWith("Bearer eyJ");
        assertThat(OPA.recues().get(OPA.recues().size() - 1).corps()).contains("\"restreinte\":false").contains("\"acr\":\"2\"");
        // Jeton d'accès de 30 s : le rafraîchissement passe par le client de la session (web_app).
        assertThat(KEYCLOAK.octrois()).anySatisfy(o -> {
            assertThat(o.type()).isEqualTo("refresh_token");
            assertThat(o.client()).isEqualTo("web_app");
        });
        int avant = KEYCLOAK.revoques().size();
        client.mutateWith(csrf()).post().uri("/api/logout").cookie("SESSION", session).exchange().expectStatus().isOk();
        assertThat(KEYCLOAK.revoques()).hasSizeGreaterThan(avant);
        client.get().uri("/api/session").cookie("SESSION", session).exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Session restreinte : assistant relayé avec le jeton de gimac-connexion (acr 1), rafraîchi par gimac-connexion ; le reste refusé")
    void sessionRestreinte() {
        String session = connecter("nouveau", null, "PREMIER_ACCES");
        client.get().uri("/bff/des/iam/moi").cookie("SESSION", session).exchange().expectStatus().isOk();
        assertThat(OPA.recues().get(OPA.recues().size() - 1).corps()).contains("\"restreinte\":true").contains("\"acr\":\"1\"");
        assertThat(IAM.recues().stream().filter(q -> q.chemin().equals("/api/v1/moi")).findFirst().orElseThrow().autorisation()).startsWith("Bearer eyJ");
        assertThat(KEYCLOAK.octrois()).anySatisfy(o -> {
            assertThat(o.type()).isEqualTo("refresh_token");
            assertThat(o.client()).isEqualTo("gimac-connexion");
        });
        client.get().uri("/api/account").cookie("SESSION", session).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Délégation par redirection retirée : 404 ; sans session : 401 sans redirection")
    void redirectionRetiree() {
        client.get().uri("/oauth2/authorization/oidc").exchange().expectStatus().isNotFound();
        client.get().uri("/login/oauth2/code/oidc?code=a&state=b").exchange().expectStatus().isNotFound();
        client.get().uri("/api/account").exchange().expectStatus().isUnauthorized().expectHeader().doesNotExist("Location");
    }

    String connecterDepuis(String userAgent) {
        var r = client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion")
            .header(org.springframework.http.HttpHeaders.USER_AGENT, userAgent)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"identifiant\":\"agent.sna\",\"motDePasse\":\"Mot-de-passe-1\",\"code\":\"123456\",\"couche\":\"des\"}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult();
        return r.getResponseCookies().getFirst("SESSION").getValue();
    }

    @Test
    @DisplayName("mon-compte.md : historique en base (R2DBC), appareils, fermeture des autres sessions au changement de mot de passe, photos relayées")
    void monCompte() {
        String a = connecterDepuis("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0");
        String b = connecterDepuis("Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 Version/17.1 Mobile/15E148 Safari/604.1");
        // Historique persistant : les deux connexions réussies, la plus récente d'abord.
        client
            .get()
            .uri("/api/session/historique?taille=2")
            .cookie("SESSION", a)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.contenu[0].resultat")
            .isEqualTo("REUSSIE")
            .jsonPath("$.contenu[0].appareil.navigateur")
            .isEqualTo("Safari 17")
            .jsonPath("$.contenu.length()")
            .isEqualTo(2);
        client.get().uri("/api/session/appareils").cookie("SESSION", a).exchange().expectStatus().isOk().expectBody().jsonPath("$[0].courante").isEqualTo(true);

        // Mot de passe changé avec « fermerAutresSessions » : MS-IAM le signale, la passerelle ferme B et retire l'en-tête.
        IAM.repondre("/api/v1/moi/mot-de-passe", 204, "application/json", "", java.util.Map.of("X-Gimac-Fermer-Autres-Sessions", "1"));
        client
            .mutateWith(csrf())
            .post()
            .uri("/bff/des/iam/moi/mot-de-passe")
            .cookie("SESSION", a)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"actuel\":\"Mot-de-passe-1\",\"nouveau\":\"Mot-de-passe-2!\",\"fermerAutresSessions\":true}")
            .exchange()
            .expectStatus()
            .isNoContent()
            .expectHeader()
            .doesNotExist("X-Gimac-Fermer-Autres-Sessions");
        client.get().uri("/api/session").cookie("SESSION", b).exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/session").cookie("SESSION", a).exchange().expectStatus().isOk();

        // Photos : celle du titulaire et celle d'un collègue relayées vers MS-IAM.
        IAM.repondre("/api/v1/avatars/u-collegue", 200, "image/png", "png", java.util.Map.of("Cache-Control", "private, max-age=86400, immutable"));
        client
            .get()
            .uri("/bff/des/iam/avatars/u-collegue?v=0123456789abcdef")
            .cookie("SESSION", a)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals("Cache-Control", "private, max-age=86400, immutable");
        assertThat(IAM.recues().stream().filter(q -> q.chemin().equals("/api/v1/avatars/u-collegue")).findFirst().orElseThrow().autorisation()).startsWith("Bearer eyJ");
    }
}
