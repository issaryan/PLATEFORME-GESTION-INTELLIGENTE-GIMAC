package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.config.ConnexionConfiguration;
import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import cm.gimac.des.gateway.infrastructure.appareils.HistoriqueConnexionsMemoire;
import cm.gimac.des.gateway.test.util.HorlogeReglable;
import cm.gimac.des.gateway.test.util.KeycloakSimule;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import tech.jhipster.config.JHipsterProperties;

/**
 * Appareils connectés et historique des connexions (docs/lot1/mon-compte.md §3) sur la vraie chaîne de
 * sécurité de la passerelle, sessions ouvertes par la vraie connexion (Keycloak et MS-IAM simulés) :
 * sécurité de chaque route (401, 403 sans second facteur, 403 en session restreinte), appareil
 * déduit du User-Agent, session courante marquée, adresses masquées, fermeture d'une autre session
 * (409 pour la courante), fermeture de toutes les autres, historique paginé des tentatives (réussies,
 * mot de passe faux, code faux, bloquées), alerte « nouvel appareil » transmise à MS-IAM.
 */
@WebFluxTest(
    controllers = { ConnexionResource.class, SessionResource.class, LogoutResource.class, AppareilsResource.class },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, ConnexionConfiguration.class, AppareilsResourceSecuriteTest.Simulations.class })
@TestPropertySource(properties = { "gimac.session.balayage=PT1H" })
@org.junit.jupiter.api.Timeout(120)
class AppareilsResourceSecuriteTest {

    static final KeycloakSimule KEYCLOAK = new KeycloakSimule();
    static final ServeurHttpLocal IAM;
    static final HorlogeReglable HORLOGE = new HorlogeReglable(Instant.now());

    static final String FIREFOX_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0";
    static final String FIREFOX_WINDOWS_MAJ = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:132.0) Gecko/20100101 Firefox/132.0";
    static final String SAFARI_IPHONE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1";
    static final String ETAT = "/api/v1/interne/comptes/%s/etat-connexion";
    static final String CONFIGURE = "{\"actif\":true,\"secondFacteur\":true,\"actionsRequises\":[]}";
    static final List<String> ROUTES_LECTURE = List.of("/api/session/appareils", "/api/session/historique");

    static {
        try {
            IAM = new ServeurHttpLocal();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Simulations {

        static ClientRegistration.Builder base(String id, String client, String secret) {
            String p = KEYCLOAK.emetteur() + "/protocol/openid-connect";
            return ClientRegistration.withRegistrationId(id)
                .clientId(client)
                .clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .issuerUri(KEYCLOAK.emetteur())
                .tokenUri(p + "/token")
                .jwkSetUri(p + "/certs")
                .authorizationUri(p + "/auth")
                .userInfoUri(p + "/userinfo")
                .userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", p + "/logout", "revocation_endpoint", p + "/revoke"));
        }

        @Bean
        ReactiveClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryReactiveClientRegistrationRepository(
                base("oidc", "web_app", "secret-web")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile")
                    .build(),
                base("connexion", "gimac-connexion", "secret-connexion")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile")
                    .build(),
                base("gateway-service", "gateway-service", "secret-service").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).build()
            );
        }

        @Bean
        ReactiveOAuth2AuthorizedClientService authorizedClientService(ReactiveClientRegistrationRepository r) {
            return new InMemoryReactiveOAuth2AuthorizedClientService(r);
        }

        @Bean
        ServerOAuth2AuthorizedClientRepository authorizedClientRepository(ReactiveOAuth2AuthorizedClientService s) {
            return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(s);
        }

        @Bean
        org.springframework.security.oauth2.jwt.ReactiveJwtDecoder jwtDecoder() {
            return org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.ReactiveJwtDecoder.class);
        }

        @Bean
        Clock horloge() {
            return HORLOGE;
        }
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("gimac.connexion.iam-url", IAM::url);
    }

    @AfterAll
    static void arreter() {
        KEYCLOAK.close();
        IAM.close();
    }

    @Autowired
    WebTestClient client;

    @Autowired
    HistoriqueConnexions historique;

    @MockitoBean
    GatewayAuditEventPublisher audit;

    @Autowired
    cm.gimac.des.gateway.security.session.RegistreSessions registre;

    @Autowired
    cm.gimac.des.gateway.security.session.FermetureSession fermeture;

    @BeforeEach
    void preparer() {
        // Chaque cas part sans session ouverte (le registre est partagé par le contexte).
        registre.sessions().forEach(s -> fermeture.fermer(s, false).block());
        // Premier appel (clés RSA, découverte) : délai élargi sur une machine chargée.
        client = client.mutate().responseTimeout(Duration.ofSeconds(30)).build();
        KEYCLOAK.reinitialiser();
        HORLOGE.avancer(Duration.ofMinutes(2));
        KEYCLOAK.compte("agent.sna", KeycloakSimule.interne("u-sna", "ROLE_SNA", "HOM_LIAISON_READ"));
        KEYCLOAK.compte("autre.agent", KeycloakSimule.interne("u-autre", "ROLE_SNA"));
        KEYCLOAK.compte("nouveau", KeycloakSimule.interne("u-nouveau", "ROLE_SNA"));
        IAM.json(ETAT.formatted("u-sna"), 200, CONFIGURE);
        IAM.json(ETAT.formatted("u-autre"), 200, CONFIGURE);
        IAM.json(ETAT.formatted("u-nouveau"), 200, "{\"actif\":true,\"secondFacteur\":false,\"actionsRequises\":[\"FICHE\"]}");
        IAM.json("/api/v1/interne/comptes/u-sna/alerte-connexion", 204, "");
        IAM.recues().clear();
    }

    static String corps(String identifiant, String motDePasse, String code) {
        return "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"" + (code == null ? "" : ",\"code\":\"" + code + "\"") + ",\"couche\":\"des\"}";
    }

    WebTestClient.ResponseSpec connecter(String corps, String userAgent) {
        // Chaque connexion après la recharge de la limitation (5 tentatives par minute).
        HORLOGE.avancer(Duration.ofSeconds(61));
        return client.mutateWith(csrf()).post().uri("/api/connexion").header(HttpHeaders.USER_AGENT, userAgent).contentType(MediaType.APPLICATION_JSON).bodyValue(corps).exchange();
    }

    String session(String identifiant, String userAgent, String etat) {
        ResponseCookie temoin = connecter(corps(identifiant, "Mot-de-passe-1", "123456"), userAgent)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.etat")
            .isEqualTo(etat)
            .returnResult()
            .getResponseCookies()
            .getFirst("SESSION");
        assertThat(temoin).isNotNull();
        return temoin.getValue();
    }

    WebTestClient.ResponseSpec avec(HttpMethod methode, String uri, String session) {
        return client.mutateWith(csrf()).method(methode).uri(uri).cookie("SESSION", session).exchange();
    }

    String lire(String uri, String session) {
        return new String(avec(HttpMethod.GET, uri, session).expectStatus().isOk().expectBody().returnResult().getResponseBody(), java.nio.charset.StandardCharsets.UTF_8);
    }

    long alertes() {
        return IAM.recues().stream().filter(r -> r.chemin().endsWith("/alerte-connexion")).count();
    }

    void attendreAlertes(long n) throws InterruptedException {
        long fin = System.currentTimeMillis() + 5000;
        while (alertes() < n && System.currentTimeMillis() < fin) {
            Thread.sleep(50);
        }
        assertThat(alertes()).isEqualTo(n);
    }

    @Test
    @DisplayName("Sécurité : 401 sans session ; 403 sans second facteur (acr 1) ; 403 CNX_SESSION_RESTREINTE au premier accès")
    void securite() {
        for (String uri : ROUTES_LECTURE) {
            client.get().uri(uri).exchange().expectStatus().isUnauthorized();
            client
                .mutateWith(mockOidcLogin().idToken(t -> t.claim("sub", "u-sna").claim("acr", "1")))
                .get()
                .uri(uri)
                .exchange()
                .expectStatus()
                .isForbidden();
        }
        for (String uri : List.of("/api/session/appareils", "/api/session/appareils/x")) {
            client.mutateWith(csrf()).delete().uri(uri).exchange().expectStatus().isUnauthorized();
            client.mutateWith(csrf()).mutateWith(mockOidcLogin().idToken(t -> t.claim("sub", "u-sna").claim("acr", "1"))).delete().uri(uri).exchange().expectStatus().isForbidden();
        }
        // Sans le témoin XSRF : les fermetures sont refusées (CSRF).
        String a = session("agent.sna", FIREFOX_WINDOWS, "CONNECTE");
        client.delete().uri("/api/session/appareils").cookie("SESSION", a).exchange().expectStatus().isForbidden();
        // Session restreinte du premier accès : aucune de ces routes.
        String restreinte = session("nouveau", FIREFOX_WINDOWS, "PREMIER_ACCES");
        for (String uri : ROUTES_LECTURE) {
            avec(HttpMethod.GET, uri, restreinte).expectStatus().isForbidden().expectBody().jsonPath("$.code").isEqualTo("CNX_SESSION_RESTREINTE");
        }
        avec(HttpMethod.DELETE, "/api/session/appareils", restreinte).expectStatus().isForbidden();
        // Acr 2 (session simulée sans connexion de la plateforme) : liste vide, accès admis.
        client
            .mutateWith(mockOidcLogin().idToken(t -> t.claim("sub", "u-simule").claim("acr", "2")))
            .get()
            .uri("/api/session/appareils")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(0);
    }

    @Test
    @DisplayName("Appareils : sessions du compte, courante marquée, adresse masquée hors courante, appareil déduit du User-Agent ; alerte « nouvel appareil »")
    void appareilsEtFermetures() throws Exception {
        String a = session("agent.sna", FIREFOX_WINDOWS, "CONNECTE");
        String b = session("agent.sna", SAFARI_IPHONE, "CONNECTE");
        String c = session("agent.sna", FIREFOX_WINDOWS_MAJ, "CONNECTE");
        String autre = session("autre.agent", FIREFOX_WINDOWS, "CONNECTE");
        // Première connexion du compte : pas d'alerte ; iPhone jamais vu : alerte ; Firefox mis à jour : pas d'alerte.
        attendreAlertes(1);
        var alerte = IAM.recues().stream().filter(r -> r.chemin().equals("/api/v1/interne/comptes/u-sna/alerte-connexion")).findFirst().orElseThrow();
        assertThat(alerte.autorisation()).startsWith("Bearer ");
        assertThat(alerte.corps()).contains("\"navigateur\":\"Safari 17\"").contains("\"systeme\":\"iOS 17\"").contains("\"type\":\"MOBILE\"").contains("\"couche\":\"des\"");

        String vue = lire("/api/session/appareils", a);
        List<Map<String, Object>> sessions = JsonPath.read(vue, "$");
        assertThat(sessions).hasSize(3);
        assertThat(sessions.get(0).get("courante")).isEqualTo(true);
        // Client de test sans adresse distante : « inconnue », masquée en « * » hors de la session courante
        // (masquage des adresses IPv4 et IPv6 vérifié par AppareilsDomaineTest).
        assertThat(sessions.get(0).get("adresseIp")).isEqualTo("inconnue");
        assertThat(sessions.subList(1, 3)).allSatisfy(s -> {
            assertThat(s.get("courante")).isEqualTo(false);
            assertThat(s.get("adresseIp")).isEqualTo("*");
        });
        assertThat((List<String>) JsonPath.read(vue, "$[*].appareil.navigateur")).containsExactlyInAnyOrder("Firefox 131", "Safari 17", "Firefox 132");
        assertThat((List<String>) JsonPath.read(vue, "$[*].appareil.type")).containsExactlyInAnyOrder("ORDINATEUR", "MOBILE", "ORDINATEUR");
        assertThat((String) JsonPath.read(vue, "$[0].appareil.systeme")).isEqualTo("Windows 10/11");
        assertThat((String) JsonPath.read(vue, "$[0].couche")).isEqualTo("des");
        assertThat((String) JsonPath.read(vue, "$[0].ouverteLe")).isNotBlank();
        assertThat((String) JsonPath.read(vue, "$[0].expireA")).isNotBlank();
        assertThat(vue).doesNotContain(a).doesNotContain(b).doesNotContain(c);
        String idA = JsonPath.read(vue, "$[0].id");
        List<String> idsSafari = JsonPath.read(vue, "$[?(@.appareil.navigateur == 'Safari 17')].id");
        String idB = idsSafari.get(0);

        // Session courante : 409 ; inconnue ou d'un autre compte : 404.
        avec(HttpMethod.DELETE, "/api/session/appareils/" + idA, a)
            .expectStatus()
            .isEqualTo(409)
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("CNX_SESSION_COURANTE");
        avec(HttpMethod.DELETE, "/api/session/appareils/inconnu", a).expectStatus().isNotFound().expectBody().jsonPath("$.code").isEqualTo("CNX_APPAREIL_INCONNU");
        avec(HttpMethod.DELETE, "/api/session/appareils/" + idB, autre).expectStatus().isNotFound();
        assertThat((List<?>) JsonPath.read(lire("/api/session/appareils", autre), "$")).hasSize(1);

        // Fermeture d'une autre session : jeton révoqué, la session B ne sert plus.
        int revoques = KEYCLOAK.revoques().size();
        avec(HttpMethod.DELETE, "/api/session/appareils/" + idB, a).expectStatus().isNoContent();
        assertThat(KEYCLOAK.revoques()).hasSize(revoques + 1);
        avec(HttpMethod.GET, "/api/session", b).expectStatus().isUnauthorized();
        assertThat((List<?>) JsonPath.read(lire("/api/session/appareils", a), "$")).hasSize(2);

        // Toutes les autres : C fermée, A et l'autre compte intacts.
        avec(HttpMethod.DELETE, "/api/session/appareils", a).expectStatus().isNoContent();
        avec(HttpMethod.GET, "/api/session", c).expectStatus().isUnauthorized();
        avec(HttpMethod.GET, "/api/session", a).expectStatus().isOk();
        avec(HttpMethod.GET, "/api/session", autre).expectStatus().isOk();
        assertThat((List<?>) JsonPath.read(lire("/api/session/appareils", a), "$")).hasSize(1);
        avec(HttpMethod.DELETE, "/api/session/appareils", a).expectStatus().isNoContent();
    }

    @Test
    @DisplayName("Historique : tentatives réussies et refusées (mot de passe, code, blocage), les plus récentes d'abord, paginé, adresses masquées")
    void historiqueDesConnexions() {
        ((HistoriqueConnexionsMemoire) historique).purger(Instant.MAX).block();
        String a = session("agent.sna", FIREFOX_WINDOWS, "CONNECTE");
        connecter(corps("agent.sna", "Mauvais-1", null), SAFARI_IPHONE).expectStatus().isUnauthorized();
        connecter(corps("agent.sna", "Mot-de-passe-1", "000000"), SAFARI_IPHONE).expectStatus().isUnauthorized();
        connecter(corps("agent.sna", "Mot-de-passe-1", null), SAFARI_IPHONE).expectStatus().isOk().expectBody().jsonPath("$.etat").isEqualTo("CODE_REQUIS");
        // Limitation : six tentatives dans la même minute, la sixième est bloquée.
        for (int i = 0; i < 5; i++) {
            client.mutateWith(csrf()).post().uri("/api/connexion").contentType(MediaType.APPLICATION_JSON).bodyValue(corps("agent.sna", "Mauvais-" + i, null)).exchange();
        }
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(corps("agent.sna", "Mauvais", null))
            .exchange()
            .expectStatus()
            .isEqualTo(429);
        // Tentatives d'un autre compte : absentes.
        session("autre.agent", FIREFOX_WINDOWS, "CONNECTE");

        String page = lire("/api/session/historique?page=0&taille=50", a);
        List<String> resultats = JsonPath.read(page, "$.contenu[*].resultat");
        assertThat(resultats).contains("REUSSIE", "ECHEC_MOT_DE_PASSE", "ECHEC_CODE", "BLOQUEE").doesNotContain("CODE_REQUIS");
        assertThat(resultats.get(resultats.size() - 1)).isEqualTo("REUSSIE");
        assertThat(((Number) JsonPath.read(page, "$.total")).intValue()).isEqualTo(resultats.size()).isEqualTo(9);
        assertThat((List<String>) JsonPath.read(page, "$.contenu[*].adresseIp")).containsOnly("*");
        assertThat((String) JsonPath.read(page, "$.contenu[?(@.resultat == 'ECHEC_CODE')].appareil.navigateur").toString()).contains("Safari 17");
        assertThat(page).doesNotContain("agent.sna").doesNotContain("Mot-de-passe").doesNotContain("000000");
        String deux = lire("/api/session/historique?page=1&taille=2", a);
        assertThat((List<?>) JsonPath.read(deux, "$.contenu")).hasSize(2);
        assertThat(((Number) JsonPath.read(deux, "$.total")).intValue()).isEqualTo(9);
        // Bornes : taille ramenée à 1..100, page négative à 0.
        assertThat((List<?>) JsonPath.read(lire("/api/session/historique?page=-3&taille=0", a), "$.contenu")).hasSize(1);
    }
}
