package cm.gimac.des.gateway.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.domain.supervision.EnvironnementService;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.domain.supervision.ServiceCible;
import cm.gimac.des.gateway.infrastructure.supervision.ActuatorWebClientAdapter;
import cm.gimac.des.gateway.infrastructure.supervision.ClientsSupervision;
import java.time.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.test.util.KeycloakSimule;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import cm.gimac.des.gateway.web.rest.AuthInfoResource;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.autoconfigure.actuate.web.WebFluxEndpointManagementContextConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import tech.jhipster.config.JHipsterProperties;

/**
 * Points /management de la passerelle lus par la supervision avec le jeton de service du client
 * « gateway-supervision » (client_credentials), sur la vraie chaîne de sécurité et le vrai
 * décodeur de jetons, Keycloak simulé : userinfo refuse ce jeton (403, comme Keycloak), ce refus
 * ne doit ni être demandé ni produire une erreur 500.
 */
@WebFluxTest(
    controllers = AuthInfoResource.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class))
@Import({ SecurityConfiguration.class, GestionJetonServiceTest.Simulations.class })
@ImportAutoConfiguration(
    {
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        EnvironmentEndpointAutoConfiguration.class,
        LoggersEndpointAutoConfiguration.class,
        WebFluxEndpointManagementContextConfiguration.class,
    }
)
// Audience de production (application.yml) : celle des jetons d'accès de Keycloak.
@TestPropertySource(
    properties = {
        "jhipster.security.oauth2.audience=account,api://default",
        "management.endpoints.web.base-path=/management",
        "management.endpoints.web.exposure.include=env,loggers",
    }
)
@org.junit.jupiter.api.Timeout(120)
class GestionJetonServiceTest {

    static final KeycloakSimule KEYCLOAK = new KeycloakSimule();

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Simulations {

        @Bean
        ReactiveClientRegistrationRepository clientRegistrationRepository() {
            String p = KEYCLOAK.emetteur() + "/protocol/openid-connect";
            return new InMemoryReactiveClientRegistrationRepository(
                ClientRegistration.withRegistrationId("oidc")
                    .clientId("web_app")
                    .clientSecret("secret-web")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .issuerUri(KEYCLOAK.emetteur())
                    .tokenUri(p + "/token")
                    .jwkSetUri(p + "/certs")
                    .authorizationUri(p + "/auth")
                    .userInfoUri(p + "/userinfo")
                    .userNameAttributeName("sub")
                    .scope("openid", "profile")
                    .build()
            );
        }
    }

    @AfterAll
    static void arreter() {
        KEYCLOAK.close();
    }

    @Autowired
    WebTestClient client;

    @Autowired
    ApplicationContext contexte;

    @BeforeEach
    void preparer() {
        KEYCLOAK.reinitialiser();
    }

    /** Jeton d'accès client_credentials tel que l'émet le domaine gimac (sans given_name ni portée openid). */
    static String jetonService(String client, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", KEYCLOAK.emetteur());
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("aud", List.of("account"));
        claims.put("azp", client);
        claims.put("preferred_username", "service-account-" + client);
        claims.put("scope", "profile email");
        claims.put("roles", List.of(roles));
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(300));
        return KEYCLOAK.signer(claims, false);
    }

    @Test
    void environnementLuAvecLeJetonDeSupervision() {
        client
            .get()
            .uri("/management/env")
            .headers(h -> h.setBearerAuth(jetonService("gateway-supervision", "default-roles-gimac", AuthoritiesConstants.ADMIN)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.propertySources")
            .isArray();
        assertThat(KEYCLOAK.appelsUserinfo()).isZero();
    }

    @Test
    void loggersLusAvecLeJetonDeSupervision() {
        client
            .get()
            .uri("/management/loggers")
            .headers(h -> h.setBearerAuth(jetonService("gateway-supervision", AuthoritiesConstants.ADMIN)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.levels")
            .isArray();
        assertThat(KEYCLOAK.appelsUserinfo()).isZero();
    }

    /**
     * Chemin réel de la console : l'adaptateur de supervision lit /management/env et /management/loggers
     * de la passerelle sur un vrai port HTTP, avec le jeton du client « gateway-supervision ».
     */
    @Test
    void adaptateurDeSupervisionSurLaPasserelle() {
        HttpHandler gestionnaire = WebHttpHandlerBuilder.applicationContext(contexte).build();
        DisposableServer serveur = HttpServer.create().host("127.0.0.1").port(0).handle(new ReactorHttpHandlerAdapter(gestionnaire)).bindNow();
        try {
            ServiceCible passerelle = new ServiceCible("gateway", "http://127.0.0.1:" + serveur.port());
            String jeton = jetonService("gateway-supervision", "default-roles-gimac", AuthoritiesConstants.ADMIN);
            ActuatorWebClientAdapter adaptateur = new ActuatorWebClientAdapter(
                ClientsSupervision.webClient(Duration.ofSeconds(10), null),
                () -> Mono.just(jeton)
            );

            EnvironnementService environnement = adaptateur.environnement(passerelle).block(Duration.ofSeconds(20));
            NiveauxJournalisation journalisation = adaptateur.journalisation(passerelle).block(Duration.ofSeconds(20));

            assertThat(environnement.proprietes()).containsKey("management.endpoints.web.base-path");
            assertThat(journalisation.niveauxDisponibles()).contains("INFO", "DEBUG");
            assertThat(journalisation.loggers()).isNotEmpty();
            assertThat(KEYCLOAK.appelsUserinfo()).isZero();
        } finally {
            serveur.disposeNow();
        }
    }

    @Test
    void jetonDeServiceSansRoleAdminRefuseEn403() {
        client
            .get()
            .uri("/management/env")
            .headers(h -> h.setBearerAuth(jetonService("gateway-service", "INTERNAL_IAM_READ")))
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Test
    void jetonUtilisateurUserinfoRefuseRepliSurLesClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", KEYCLOAK.emetteur());
        claims.put("sub", "u-admin-" + UUID.randomUUID());
        claims.put("aud", List.of("account"));
        claims.put("azp", "web_app");
        claims.put("preferred_username", "admin.plateforme");
        claims.put("roles", List.of(AuthoritiesConstants.ADMIN));
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(300));

        client
            .get()
            .uri("/management/env")
            .headers(h -> h.setBearerAuth(KEYCLOAK.signer(claims, false)))
            .exchange()
            .expectStatus()
            .isOk();
        assertThat(KEYCLOAK.appelsUserinfo()).isEqualTo(1);
    }

    @Test
    void jetonSigneParUneAutreCleRefuseEn401() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", KEYCLOAK.emetteur());
        claims.put("sub", "x");
        claims.put("aud", List.of("account"));
        claims.put("preferred_username", "service-account-gateway-supervision");
        claims.put("roles", List.of(AuthoritiesConstants.ADMIN));
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(300));

        client
            .get()
            .uri("/management/env")
            .headers(h -> h.setBearerAuth(KEYCLOAK.signer(claims, true)))
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }
}
