package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import cm.gimac.des.gateway.audit.GatewayAuditEventPublisher;
import cm.gimac.des.gateway.config.ConnexionConfiguration;
import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.security.session.FermetureSession;
import cm.gimac.des.gateway.test.util.HorlogeReglable;
import cm.gimac.des.gateway.test.util.KeycloakSimule;
import cm.gimac.des.gateway.test.util.ServeurHttpLocal;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import tech.jhipster.config.JHipsterProperties;

/**
 * Connexion sur la plateforme (D-65, docs/lot1/connexion.md §2 et §6) sur la vraie chaîne de
 * sécurité de la passerelle, Keycloak et MS-IAM simulés par des serveurs HTTP locaux : états
 * CODE_REQUIS, PREMIER_ACCES et CONNECTE, codes d'erreur 400/401/403/429/503 en problem+json,
 * CSRF, régénération de l'identifiant de session, session restreinte, échéances (inactivité, appels
 * de fond, durée absolue, balayage), déconnexion, délégation par redirection retirée, audit.
 */
@WebFluxTest(
    controllers = { ConnexionResource.class, SessionResource.class, LogoutResource.class },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, ConnexionConfiguration.class, ConnexionResourceSecuriteTest.Simulations.class })
@TestPropertySource(properties = { "gimac.session.balayage=PT1H" })
// Démarrage du contexte WebFlux et clés RSA du Keycloak simulé : délai élargi sur une machine chargée.
@org.junit.jupiter.api.Timeout(120)
class ConnexionResourceSecuriteTest {

    static final KeycloakSimule KEYCLOAK = new KeycloakSimule();
    static final ServeurHttpLocal IAM;
    static final HorlogeReglable HORLOGE = new HorlogeReglable(Instant.now());

    static {
        try {
            IAM = new ServeurHttpLocal();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static final String ETAT = "/api/v1/interne/comptes/%s/etat-connexion";
    static final String CONFIGURE = "{\"actif\":true,\"secondFacteur\":true,\"actionsRequises\":[]}";

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

        /** Jetons porteurs hors sujet ici (la session est ouverte par la connexion). */
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
    FermetureSession fermeture;

    @MockitoBean
    GatewayAuditEventPublisher audit;

    @BeforeEach
    void preparer() {
        KEYCLOAK.reinitialiser();
        // Recharge complète de la limitation (5 par minute) entre deux cas.
        HORLOGE.avancer(Duration.ofMinutes(2));
        KEYCLOAK.compte("agent.sna", KeycloakSimule.interne("u-sna", "ROLE_SNA", "HOM_LIAISON_READ"));
        KEYCLOAK.compte("corr.banque", KeycloakSimule.correspondant("u-corr", "INST-0042", "ROLE_CORRESPONDANT", "HOM_PORTAL_READ"));
        KEYCLOAK.compte("nouveau", KeycloakSimule.interne("u-nouveau", "ROLE_SNA"));
        KEYCLOAK.compte("inactif.kc", new KeycloakSimule.Compte("u-ina", "Mot-de-passe-1", null, false, Map.of()));
        IAM.json(ETAT.formatted("u-sna"), 200, CONFIGURE);
        IAM.json(ETAT.formatted("u-corr"), 200, CONFIGURE);
        IAM.json(ETAT.formatted("u-nouveau"), 200, "{\"actif\":true,\"secondFacteur\":false,\"actionsRequises\":[\"FICHE\",\"MOT_DE_PASSE\"]}");
        clearInvocations(audit);
    }

    static String corps(String identifiant, String motDePasse, String code, String couche) {
        return (
            "{\"identifiant\":\"" + identifiant + "\",\"motDePasse\":\"" + motDePasse + "\"" + (code == null ? "" : ",\"code\":\"" + code + "\"") +
            ",\"couche\":\"" + couche + "\"}"
        );
    }

    WebTestClient.ResponseSpec connecter(String corps, String session) {
        WebTestClient.RequestBodySpec r = client.mutateWith(csrf()).post().uri("/api/connexion").contentType(MediaType.APPLICATION_JSON);
        if (session != null) {
            r = r.cookie("SESSION", session);
        }
        return r.bodyValue(corps).exchange();
    }

    /** Connexion réussie : identifiant de la session ouverte. */
    String session(String corps, String etatAttendu, String sessionPrecedente) {
        EntityExchangeResult<byte[]> r = connecter(corps, sessionPrecedente)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.etat")
            .isEqualTo(etatAttendu)
            .returnResult();
        ResponseCookie temoin = r.getResponseCookies().getFirst("SESSION");
        assertThat(temoin).isNotNull();
        // Attribut Secure (server.reactive.session.cookie.secure) vérifié par ConnexionIT, contexte complet.
        assertThat(temoin.isHttpOnly()).isTrue();
        assertThat(temoin.getSameSite()).isEqualToIgnoringCase("Lax");
        return temoin.getValue();
    }

    WebTestClient.ResponseSpec lireSession(String session) {
        return client.get().uri("/api/session").cookie("SESSION", session).exchange();
    }

    static void probleme(WebTestClient.ResponseSpec r, int statut, String code) {
        r.expectStatus().isEqualTo(statut).expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).expectBody().jsonPath("$.code").isEqualTo(code).jsonPath("$.status").isEqualTo(statut);
    }

    @Test
    @DisplayName("Compte configuré, sans code : CODE_REQUIS, aucune session, jeton de gimac-connexion révoqué")
    void codeRequis() {
        EntityExchangeResult<byte[]> r = connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.etat")
            .isEqualTo("CODE_REQUIS")
            .jsonPath("$.session")
            .doesNotExist()
            .returnResult();
        assertThat(r.getResponseCookies().getFirst("SESSION")).isNull();
        assertThat(KEYCLOAK.octrois()).extracting(KeycloakSimule.Octroi::client).containsExactly("gimac-connexion");
        assertThat(KEYCLOAK.revoques()).hasSize(1);
        // Le jeton de service gateway-service a été présenté à MS-IAM.
        assertThat(IAM.recues().stream().filter(q -> q.chemin().equals(ETAT.formatted("u-sna"))).findFirst().orElseThrow().autorisation()).startsWith("Bearer ");
    }

    @Test
    @DisplayName("Mot de passe et code : CONNECTE, session à deux facteurs, témoin HttpOnly SameSite=Lax, aucun jeton renvoyé")
    void connecte() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        assertThat(KEYCLOAK.octrois()).extracting(KeycloakSimule.Octroi::client).containsExactly("gimac-connexion", "web_app");
        assertThat(KEYCLOAK.octrois().get(1).avecCode()).isTrue();
        // Le jeton de gimac-connexion ne survit pas à la session complète.
        assertThat(KEYCLOAK.revoques()).hasSize(1);
        String reponse = new String(
            lireSession(session)
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.acr")
                .isEqualTo("2")
                .jsonPath("$.doubleFacteur")
                .isEqualTo(true)
                .jsonPath("$.restreinte")
                .isEqualTo(false)
                .jsonPath("$.identifiant")
                .isEqualTo("agent.sna")
                .jsonPath("$.sujet")
                .isEqualTo("u-sna")
                .jsonPath("$.roles[0]")
                .isEqualTo("ROLE_SNA")
                .jsonPath("$.permissions[0]")
                .isEqualTo("HOM_LIAISON_READ")
                .jsonPath("$.expireA")
                .isEqualTo(HORLOGE.instant().plus(Duration.ofMinutes(30)).toString())
                .returnResult()
                .getResponseBody()
        );
        assertThat(reponse).doesNotContain("eyJ").doesNotContain("rt-");
        verify(audit, timeout(2000)).publishSecurityEvent(eq("gateway.login-succeeded.v1"), eq("u-sna"), anyString(), eq(200), any());
    }

    @Test
    @DisplayName("Réponse CONNECTE : vue de la session incluse")
    void connecteVue() {
        connecter(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), null)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.session.acr")
            .isEqualTo("2")
            .jsonPath("$.session.restreinte")
            .isEqualTo(false)
            .jsonPath("$.etapes")
            .doesNotExist();
    }

    @Test
    @DisplayName("Code faux : 401 CNX_IDENTIFIANTS_INVALIDES, aucun jeton conservé")
    void codeFaux() {
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", "000000", "des"), null), 401, "CNX_IDENTIFIANTS_INVALIDES");
        assertThat(KEYCLOAK.revoques()).hasSize(1);
        verify(audit, timeout(2000)).publishSecurityEvent(eq("gateway.login-failed.v1"), isNull(), anyString(), eq(401), any());
    }

    @Test
    @DisplayName("Mot de passe faux ou compte inconnu : 401 générique ; journal sans mot de passe, identifiant masqué")
    void identifiantsInvalides() {
        probleme(connecter(corps("agent.sna", "mauvais", null, "des"), null), 401, "CNX_IDENTIFIANTS_INVALIDES");
        probleme(connecter(corps("inconnu", "Mot-de-passe-1", null, "des"), null), 401, "CNX_IDENTIFIANTS_INVALIDES");
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, String>> details = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(audit, timeout(2000).times(2)).publishSecurityEvent(eq("gateway.login-failed.v1"), isNull(), anyString(), eq(401), details.capture());
        assertThat(details.getAllValues().get(0)).containsEntry("identifiant", "ag***a").containsEntry("code", "CNX_IDENTIFIANTS_INVALIDES").containsEntry("couche", "des");
        assertThat(details.getAllValues().toString()).doesNotContain("mauvais").doesNotContain("Mot-de-passe-1").doesNotContain("agent.sna");
    }

    @Test
    @DisplayName("Compte désactivé dans Keycloak : 403 CNX_COMPTE_INACTIF")
    void compteDesactive() {
        probleme(connecter(corps("inactif.kc", "Mot-de-passe-1", null, "des"), null), 403, "CNX_COMPTE_INACTIF");
    }

    @Test
    @DisplayName("Compte inactif selon MS-IAM, ou inconnu de MS-IAM : 403 CNX_COMPTE_INACTIF, jeton révoqué")
    void compteInactifIam() {
        IAM.json(ETAT.formatted("u-sna"), 200, "{\"actif\":false,\"secondFacteur\":true,\"actionsRequises\":[]}");
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), null), 403, "CNX_COMPTE_INACTIF");
        IAM.json(ETAT.formatted("u-sna"), 404, "{}");
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), null), 403, "CNX_COMPTE_INACTIF");
        assertThat(KEYCLOAK.revoques()).hasSize(2);
        assertThat(KEYCLOAK.octrois()).extracting(KeycloakSimule.Octroi::client).doesNotContain("web_app");
    }

    @Test
    @DisplayName("Couche : le correspondant n'ouvre que le portail, l'acteur interne pas le portail (403 CNX_COUCHE_REFUSEE)")
    void couche() {
        probleme(connecter(corps("corr.banque", "Mot-de-passe-1", "123456", "des"), null), 403, "CNX_COUCHE_REFUSEE");
        probleme(connecter(corps("corr.banque", "Mot-de-passe-1", "123456", "console"), null), 403, "CNX_COUCHE_REFUSEE");
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", "123456", "portail"), null), 403, "CNX_COUCHE_REFUSEE");
        String session = session(corps("corr.banque", "Mot-de-passe-1", "123456", "portail"), "CONNECTE", null);
        lireSession(session).expectStatus().isOk().expectBody().jsonPath("$.institutionId").isEqualTo("INST-0042");
    }

    @Test
    @DisplayName("Corps incomplet ou couche inconnue : 400 CNX_REQUETE_INVALIDE, Keycloak non appelé")
    void requeteInvalide() {
        probleme(connecter("{}", null), 400, "CNX_REQUETE_INVALIDE");
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", null, "autre"), null), 400, "CNX_REQUETE_INVALIDE");
        probleme(connecter(corps("agent.sna", " ", null, "des"), null), 400, "CNX_REQUETE_INVALIDE");
        assertThat(KEYCLOAK.octrois()).isEmpty();
    }

    @Test
    @DisplayName("Keycloak ou MS-IAM indisponible : 503 CNX_SERVICE_INDISPONIBLE")
    void indisponible() {
        KEYCLOAK.panne(503);
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null), 503, "CNX_SERVICE_INDISPONIBLE");
        KEYCLOAK.panne(0);
        IAM.json(ETAT.formatted("u-sna"), 500, "{}");
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null), 503, "CNX_SERVICE_INDISPONIBLE");
    }

    @Test
    @DisplayName("Limitation : 5 tentatives par minute et par identifiant, puis 429 CNX_TROP_DE_TENTATIVES")
    void limitation() {
        for (int i = 0; i < 5; i++) {
            probleme(connecter(corps("agent.sna", "faux" + i, null, "des"), null), 401, "CNX_IDENTIFIANTS_INVALIDES");
        }
        connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null).expectHeader().valueEquals("Retry-After", "60");
        probleme(connecter(corps("AGENT.SNA", "Mot-de-passe-1", null, "des"), null), 429, "CNX_TROP_DE_TENTATIVES");
        assertThat(KEYCLOAK.octrois()).hasSize(5);
        HORLOGE.avancer(Duration.ofMinutes(1));
        connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null).expectStatus().isOk();
    }

    @Test
    @DisplayName("Limitation par adresse IP : au-delà de 5 tentatives par minute, quel que soit l'identifiant")
    void limitationAdresse() {
        for (int i = 0; i < 5; i++) {
            probleme(connecter(corps("x" + i, "faux", null, "des"), null), 401, "CNX_IDENTIFIANTS_INVALIDES");
        }
        probleme(connecter(corps("agent.sna", "Mot-de-passe-1", null, "des"), null), 429, "CNX_TROP_DE_TENTATIVES");
    }

    @Test
    @DisplayName("CSRF : POST /api/connexion sans jeton XSRF refusé (403)")
    void csrfExige() {
        client
            .post()
            .uri("/api/connexion")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(corps("agent.sna", "Mot-de-passe-1", null, "des"))
            .exchange()
            .expectStatus()
            .isForbidden();
        assertThat(KEYCLOAK.octrois()).isEmpty();
    }

    @Test
    @DisplayName("Délégation par redirection retirée : /oauth2/authorization/oidc et /login/oauth2/code/oidc répondent 404")
    void redirectionRetiree() {
        client.get().uri("/oauth2/authorization/oidc").exchange().expectStatus().isNotFound();
        client.get().uri("/login/oauth2/code/oidc?code=x&state=y").exchange().expectStatus().isNotFound();
        // Sans session : 401, jamais de redirection vers le fournisseur d'identité.
        client.get().uri("/api/session").exchange().expectStatus().isUnauthorized().expectHeader().doesNotExist("Location");
        client.get().uri("/tableau").exchange().expectHeader().doesNotExist("Location");
    }

    @Test
    @DisplayName("Premier accès : session restreinte (acr 1), étapes ordonnées, seuls les chemins de l'assistant")
    void premierAcces() {
        EntityExchangeResult<byte[]> r = connecter(corps("nouveau", "Mot-de-passe-1", null, "console"), null)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.etat")
            .isEqualTo("PREMIER_ACCES")
            .jsonPath("$.etapes")
            .isEqualTo(List.of("MOT_DE_PASSE", "SECOND_FACTEUR", "FICHE"))
            .returnResult();
        String session = r.getResponseCookies().getFirst("SESSION").getValue();
        assertThat(KEYCLOAK.revoques()).isEmpty();
        lireSession(session)
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.restreinte")
            .isEqualTo(true)
            .jsonPath("$.acr")
            .isEqualTo("1")
            .jsonPath("$.doubleFacteur")
            .isEqualTo(false);
        // Hors assistant : 403 CNX_SESSION_RESTREINTE.
        probleme(client.get().uri("/api/account").cookie("SESSION", session).exchange(), 403, "CNX_SESSION_RESTREINTE");
        probleme(client.get().uri("/bff/des/homologation/liaisons").cookie("SESSION", session).exchange(), 403, "CNX_SESSION_RESTREINTE");
        probleme(client.get().uri("/bff/des/live").cookie("SESSION", session).exchange(), 403, "CNX_SESSION_RESTREINTE");
        probleme(
            client.mutateWith(csrf()).put().uri("/bff/des/iam/moi/mot-de-passe").cookie("SESSION", session).exchange(),
            403,
            "CNX_SESSION_RESTREINTE"
        );
        // Chemins de l'assistant : transmis (aucune route dans ce test : 404 après la garde et la sécurité).
        client.get().uri("/bff/des/iam/moi").cookie("SESSION", session).exchange().expectStatus().isNotFound();
        client.mutateWith(csrf()).post().uri("/bff/console/iam/moi/second-facteur/totp/enrolement").cookie("SESSION", session).exchange().expectStatus().isNotFound();
        client.mutateWith(csrf()).post().uri("/api/session/prolongation").cookie("SESSION", session).exchange().expectStatus().isNoContent();
        verify(audit, timeout(2000)).publishSecurityEvent(eq("gateway.login-succeeded.v1"), eq("u-nouveau"), anyString(), eq(200), any());
    }

    @Test
    @DisplayName("Fin du premier accès : nouvelle connexion avec code, nouvel identifiant de session, ancien jeton révoqué")
    void fixation() {
        String restreinte = session(corps("nouveau", "Mot-de-passe-1", null, "des"), "PREMIER_ACCES", null);
        IAM.json(ETAT.formatted("u-nouveau"), 200, CONFIGURE);
        String complete = session(corps("nouveau", "Mot-de-passe-1", "123456", "des"), "CONNECTE", restreinte);
        assertThat(complete).isNotEqualTo(restreinte);
        lireSession(restreinte).expectStatus().isUnauthorized();
        lireSession(complete).expectStatus().isOk().expectBody().jsonPath("$.restreinte").isEqualTo(false).jsonPath("$.acr").isEqualTo("2");
        // Jeton de la session restreinte et jeton intermédiaire de gimac-connexion révoqués.
        assertThat(KEYCLOAK.revoques()).hasSize(2);
    }

    @Test
    @DisplayName("Inactivité : 30 min sans activité, 401 CNX_SESSION_EXPIREE, jeton révoqué, témoin toujours refusé")
    void inactivite() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        int avant = KEYCLOAK.revoques().size();
        HORLOGE.avancer(Duration.ofMinutes(29));
        lireSession(session).expectStatus().isOk();
        HORLOGE.avancer(Duration.ofMinutes(30));
        probleme(lireSession(session), 401, "CNX_SESSION_EXPIREE");
        assertThat(KEYCLOAK.revoques()).hasSize(avant + 1);
        probleme(lireSession(session), 401, "CNX_SESSION_EXPIREE");
        // Une nouvelle connexion reste possible avec l'ancien témoin.
        session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", session);
    }

    @Test
    @DisplayName("Appels de fond (X-Gimac-Fond: 1) : ne prolongent pas la session")
    void appelsDeFond() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        HORLOGE.avancer(Duration.ofMinutes(20));
        client.get().uri("/api/session").cookie("SESSION", session).header("X-Gimac-Fond", "1").exchange().expectStatus().isOk();
        HORLOGE.avancer(Duration.ofMinutes(11));
        probleme(lireSession(session), 401, "CNX_SESSION_EXPIREE");
    }

    @Test
    @DisplayName("Prolongation explicite : POST /api/session/prolongation 204 repousse l'échéance")
    void prolongation() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        HORLOGE.avancer(Duration.ofMinutes(25));
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/session/prolongation")
            .cookie("SESSION", session)
            .header("X-Gimac-Fond", "1")
            .exchange()
            .expectStatus()
            .isNoContent();
        HORLOGE.avancer(Duration.ofMinutes(25));
        lireSession(session).expectStatus().isOk();
        client.mutateWith(csrf()).post().uri("/api/session/prolongation").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Durée absolue : 10 h au plus, même avec une activité régulière")
    void dureeAbsolue() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        Instant debut = HORLOGE.instant();
        for (int i = 0; i < 20; i++) {
            HORLOGE.avancer(Duration.ofMinutes(29));
            lireSession(session).expectStatus().isOk();
        }
        lireSession(session).expectBody().jsonPath("$.expireA").isEqualTo(debut.plus(Duration.ofHours(10)).toString());
        HORLOGE.avancer(Duration.ofMinutes(29));
        probleme(lireSession(session), 401, "CNX_SESSION_EXPIREE");
    }

    @Test
    @DisplayName("Balayage : la session échue est fermée sans requête, son jeton révoqué")
    void balayage() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        int avant = KEYCLOAK.revoques().size();
        HORLOGE.avancer(Duration.ofMinutes(31));
        assertThat(fermeture.balayer().block()).isPositive();
        // Les sessions échues des cas précédents sont fermées elles aussi.
        assertThat(KEYCLOAK.revoques()).hasSizeGreaterThan(avant);
        assertThat(fermeture.balayer().block()).isZero();
        probleme(lireSession(session), 401, "CNX_SESSION_EXPIREE");
    }

    @Test
    @DisplayName("Déconnexion : 200 sans adresse du fournisseur d'identité, jeton révoqué, session supprimée")
    void deconnexion() {
        String session = session(corps("agent.sna", "Mot-de-passe-1", "123456", "des"), "CONNECTE", null);
        int avant = KEYCLOAK.revoques().size();
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/logout")
            .cookie("SESSION", session)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.logoutUrl")
            .doesNotExist();
        assertThat(KEYCLOAK.revoques()).hasSize(avant + 1);
        lireSession(session).expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Mot de passe oublié : 202 toujours, transmis à MS-IAM avec la couche ; rien ne fuit en cas d'échec")
    void motDePasseOublie() {
        IAM.json("/api/v1/interne/connexion/mot-de-passe-oublie", 202, "");
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion/mot-de-passe-oublie")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"identifiant\":\"agent.sna@gimac.org\",\"couche\":\"des\"}")
            .exchange()
            .expectStatus()
            .isAccepted();
        assertThat(IAM.recues().stream().filter(q -> q.chemin().endsWith("/mot-de-passe-oublie")).reduce((a, b) -> b).orElseThrow().corps())
            .contains("agent.sna@gimac.org")
            .contains("\"couche\":\"des\"");
        IAM.json("/api/v1/interne/connexion/mot-de-passe-oublie", 500, "{}");
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion/mot-de-passe-oublie")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"identifiant\":\"inconnu\"}")
            .exchange()
            .expectStatus()
            .isAccepted();
        client.mutateWith(csrf()).post().uri("/api/connexion/mot-de-passe-oublie").exchange().expectStatus().isAccepted();
    }

    @Test
    @DisplayName("Nouveau mot de passe : 204 ; 400 de MS-IAM relayé (CNX_JETON_INVALIDE) ; 400 si incomplet ; 503 si MS-IAM est en panne")
    void nouveauMotDePasse() {
        String chemin = "/api/v1/interne/connexion/nouveau-mot-de-passe";
        IAM.repondre(chemin, 204, "application/json", "", 0);
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion/nouveau-mot-de-passe")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"jeton\":\"abc\",\"motDePasse\":\"Nouveau-mot-de-passe-12\"}")
            .exchange()
            .expectStatus()
            .isNoContent();
        IAM.repondre(chemin, 400, "application/problem+json", "{\"status\":400,\"code\":\"IAM_PASSWORD_POLICY\",\"violations\":[\"LONGUEUR\"]}", 0);
        client
            .mutateWith(csrf())
            .post()
            .uri("/api/connexion/nouveau-mot-de-passe")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"jeton\":\"abc\",\"motDePasse\":\"court\"}")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("IAM_PASSWORD_POLICY")
            .jsonPath("$.violations[0]")
            .isEqualTo("LONGUEUR");
        probleme(
            client.mutateWith(csrf()).post().uri("/api/connexion/nouveau-mot-de-passe").contentType(MediaType.APPLICATION_JSON).bodyValue("{\"jeton\":\"abc\"}").exchange(),
            400,
            "CNX_REQUETE_INVALIDE"
        );
        IAM.repondre(chemin, 500, "application/json", "{}", 0);
        probleme(
            client
                .mutateWith(csrf())
                .post()
                .uri("/api/connexion/nouveau-mot-de-passe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"jeton\":\"abc\",\"motDePasse\":\"x\"}")
                .exchange(),
            503,
            "CNX_SERVICE_INDISPONIBLE"
        );
    }

    @Test
    @DisplayName("Sondes publiques : aucune session requise")
    void sondes() {
        client.get().uri("/management/health").exchange().expectStatus().value(s -> assertThat(s).isNotEqualTo(401));
        verify(audit, org.mockito.Mockito.never()).publishSecurityEvent(anyString(), anyString(), anyString(), anyInt(), any());
    }
}
