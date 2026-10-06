package cm.gimac.des.gateway.web.rest.plateforme;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.config.TestSecurityConfiguration;
import cm.gimac.des.gateway.domain.supervision.MetriqueSerie;
import cm.gimac.des.gateway.domain.supervision.PeriodeSerie;
import cm.gimac.des.gateway.domain.supervision.SerieMetrique;
import cm.gimac.des.gateway.domain.supervision.SeriesMetriques;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.service.supervision.SeriesMetriquesService;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;

/**
 * Sécurité de GET /api/plateforme/series (D-61) : 401 sans session, 403 sans second facteur, 403 sans
 * PLATFORM_SUPERVISION_READ, 200 avec ; métrique ou période hors liste et cible invalide : 400.
 */
@WebFluxTest(
    controllers = SeriesResource.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, SeriesResourceSecurityTest.Proprietes.class })
class SeriesResourceSecurityTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Proprietes {}

    static final Instant T0 = Instant.parse("2026-10-06T09:00:00Z");
    static final String ROUTE = "/api/plateforme/series?metrique=SERVICE_LATENCE_P95&periode=SIX_HEURES";

    @Autowired
    WebTestClient client;

    @MockitoBean
    SeriesMetriquesService series;

    @BeforeEach
    void simuler() {
        when(series.series(any(), any(), any())).thenAnswer(i -> {
            if ("iam\"}".equals(i.getArgument(2))) {
                throw new IllegalArgumentException("Cible invalide");
            }
            return Mono.just(
                new SeriesMetriques(
                    i.getArgument(0),
                    "ms",
                    i.getArgument(1),
                    T0,
                    T0.plusSeconds(3600),
                    90,
                    true,
                    List.of(new SerieMetrique("iam", List.of(new SerieMetrique.Point(T0.toEpochMilli(), 12.5))))
                )
            );
        });
    }

    private WebTestClient session(String acr, String... permissions) {
        GrantedAuthority[] autorites = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new);
        return client.mutateWith(
            mockOidcLogin()
                .idToken(t -> {
                    t.subject("super-admin");
                    if (acr != null) {
                        t.claim("acr", acr);
                    }
                })
                .authorities(autorites)
        );
    }

    @Test
    @DisplayName("401 sans session ; 403 sans second facteur ; 403 sans la permission")
    void refus() {
        client.get().uri(ROUTE).exchange().expectStatus().isUnauthorized();
        session("1", AuthoritiesConstants.PLATFORM_SUPERVISION_READ).get().uri(ROUTE).exchange().expectStatus().isForbidden();
        session(null, AuthoritiesConstants.PLATFORM_SUPERVISION_READ).get().uri(ROUTE).exchange().expectStatus().isForbidden();
        session("2", AuthoritiesConstants.PLATFORM_CONFIG_READ).get().uri(ROUTE).exchange().expectStatus().isForbidden();
    }

    @Test
    @DisplayName("200 avec la permission et le second facteur : séries, unité, période")
    void autorise() {
        session("2", AuthoritiesConstants.PLATFORM_SUPERVISION_READ)
            .get()
            .uri(ROUTE)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.metrique")
            .isEqualTo("SERVICE_LATENCE_P95")
            .jsonPath("$.periode")
            .isEqualTo("SIX_HEURES")
            .jsonPath("$.unite")
            .isEqualTo("ms")
            .jsonPath("$.series[0].nom")
            .isEqualTo("iam")
            .jsonPath("$.series[0].points[0].v")
            .isEqualTo(12.5);
        verify(series).series(eq(MetriqueSerie.SERVICE_LATENCE_P95), eq(PeriodeSerie.SIX_HEURES), isNull());
        session("3", AuthoritiesConstants.PLATFORM_SUPERVISION_READ)
            .get()
            .uri("/api/plateforme/series?metrique=CONTENEUR_CPU&cible=gimac-des-iam-1")
            .exchange()
            .expectStatus()
            .isOk();
        verify(series).series(eq(MetriqueSerie.CONTENEUR_CPU), eq(PeriodeSerie.UNE_HEURE), eq("gimac-des-iam-1"));
    }

    @Test
    @DisplayName("400 : métrique ou période hors de la liste fermée, métrique absente, cible invalide")
    void validation() {
        WebTestClient s = session("2", AuthoritiesConstants.PLATFORM_SUPERVISION_READ);
        s.get().uri("/api/plateforme/series?metrique=up").exchange().expectStatus().isBadRequest();
        s.get().uri("/api/plateforme/series?metrique=HOTE_CPU&periode=P30D").exchange().expectStatus().isBadRequest();
        s.get().uri("/api/plateforme/series").exchange().expectStatus().isBadRequest();
        s
            .get()
            .uri(b -> b.path("/api/plateforme/series").queryParam("metrique", "SERVICE_DEBIT").queryParam("cible", "{c}").build("iam\"}"))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("PLATFORM_SERIES_TARGET_INVALID");
    }
}
