package cm.gimac.des.gateway.config;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.config.JHipsterProperties;

/**
 * Chemins JHipster /services/{service}/** (correctif du contournement d'OPA) : sur la vraie chaîne de
 * sécurité de la passerelle, ils sont refusés à tous, même à un jeton ou une session portant ROLE_ADMIN,
 * les permissions d'administration et un second facteur. Une sonde répond 200 sur /services/** : le refus
 * vient donc de la chaîne de sécurité, et non de l'absence de route (RoutesPresentationTest).
 */
@WebFluxTest(
    controllers = CheminsServicesSecuriteTest.Sonde.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, CheminsServicesSecuriteTest.Proprietes.class, CheminsServicesSecuriteTest.Sonde.class })
class CheminsServicesSecuriteTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Proprietes {}

    /** Répondrait 200 à tout chemin /services/** si la chaîne de sécurité le laissait passer. */
    @RestController
    static class Sonde {

        @RequestMapping({ "/services", "/services/**", "/api/temoin-services" })
        String repondre() {
            return "servi";
        }
    }

    static final String[] AUTORITES_SUPER_ADMIN = {
        AuthoritiesConstants.ADMIN,
        "ROLE_SUPER_ADMIN",
        "IAM_ACCOUNT_MANAGE",
        "IAM_ROLE_PRIVILEGED_GRANT",
        "PLATFORM_CONFIG_EDIT",
        "PAR_INSTITUTION_EDIT",
    };

    @Autowired
    WebTestClient client;

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
            "/services",
            "/services/iam/api/collaborators",
            "/services/iam/api/admin/users",
            "/services/participant/api/institutions",
            "/services/certification/management/env",
            "/services/iam/v3/api-docs",
            "/services/audit/management/health/readiness",
            "/services/gateway/api/admin/users",
        }
    )
    @DisplayName("/services/** : 401 sans session ; 403 avec un jeton porteur ou une session de super administrateur (ROLE_ADMIN, acr 2)")
    void refuseMemeAvecRoleAdmin(String chemin) {
        client.get().uri(chemin).exchange().expectStatus().isUnauthorized();

        SimpleGrantedAuthority[] autorites = java.util.Arrays.stream(AUTORITES_SUPER_ADMIN)
            .map(SimpleGrantedAuthority::new)
            .toArray(SimpleGrantedAuthority[]::new);
        WebTestClient jeton = client.mutateWith(
            mockJwt().jwt(j -> j.subject("gateway-supervision").claim("acr", "2")).authorities(autorites)
        );
        WebTestClient session = client.mutateWith(
            mockOidcLogin().idToken(t -> t.subject("super-admin").claim("preferred_username", "super-admin").claim("acr", "2")).authorities(autorites)
        );
        for (WebTestClient c : new WebTestClient[] { jeton, session }) {
            for (HttpMethod methode : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE }) {
                c.mutateWith(csrf()).method(methode).uri(chemin).exchange().expectStatus().isForbidden();
            }
        }
    }

    @Test
    @DisplayName("Témoin : le même jeton atteint la sonde hors /services (200) ; le refus vient de la règle /services/**")
    void temoin() {
        client
            .mutateWith(mockJwt().jwt(j -> j.subject("gateway-supervision").claim("acr", "2")).authorities(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
            .get()
            .uri("/api/temoin-services")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .isEqualTo("servi");
    }
}
