package cm.gimac.des.gateway.web.rest.plateforme;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.config.TestSecurityConfiguration;
import cm.gimac.des.gateway.domain.supervision.ChangementJournalisation;
import cm.gimac.des.gateway.domain.supervision.ConfigurationPlateforme;
import cm.gimac.des.gateway.domain.supervision.EtatAlertes;
import cm.gimac.des.gateway.domain.supervision.EtatGlobal;
import cm.gimac.des.gateway.domain.supervision.EtatServeurs;
import cm.gimac.des.gateway.domain.supervision.ImpulsionPlateforme;
import cm.gimac.des.gateway.domain.supervision.InstantanePlateforme;
import cm.gimac.des.gateway.domain.supervision.NiveauxJournalisation;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;

/**
 * Sécurité de /api/plateforme/** (D-61), sur la vraie chaîne de sécurité de la passerelle :
 * pour chaque route, 401 sans session, 403 sans second facteur (acr 1), 403 sans la
 * permission, 200 avec elle ; séparation lecture et modification de la configuration.
 */
@WebFluxTest(
    controllers = PlateformeResource.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, PlateformeResourceSecurityTest.Proprietes.class })
class PlateformeResourceSecurityTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Proprietes {}

    static final Instant T0 = Instant.parse("2026-09-29T10:00:00Z");
    static final String CORPS = "{\"logger\":\"cm.gimac.des\",\"niveau\":\"DEBUG\",\"motif\":\"Analyse de l'incident INC-42\"}";

    @Autowired
    WebTestClient client;

    @MockitoBean
    SupervisionPlateformeService supervision;

    @BeforeEach
    void simuler() {
        InstantanePlateforme instantane = new InstantanePlateforme(T0, new EtatGlobal(EtatGlobal.OPERATIONNELLE, 0, 0, 0, 0), List.of(), null);
        when(supervision.instantane()).thenReturn(Mono.just(instantane));
        when(supervision.etatServeurs()).thenReturn(Mono.just(new EtatServeurs(T0, true, null, 0, 0, List.of(), false, List.of(), List.of())));
        when(supervision.etatAlertes()).thenReturn(Mono.just(new EtatAlertes(T0, true, null, 0, List.of())));
        when(supervision.configuration()).thenReturn(Mono.just(new ConfigurationPlateforme(T0, List.of(), List.of())));
        when(supervision.journalisation(anyString(), any())).thenReturn(Mono.just(new NiveauxJournalisation("iam", List.of("INFO"), List.of())));
        when(supervision.modifierJournalisation(anyString(), anyString(), anyString(), anyString(), anyString())).thenAnswer(i ->
            Mono.just(new ChangementJournalisation(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4), T0))
        );
        when(supervision.flux()).thenReturn(Flux.just(new ImpulsionPlateforme(0, instantane, 0)));
    }

    /** Route, méthode et permission exigée. */
    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/plateforme/services", AuthoritiesConstants.PLATFORM_SUPERVISION_READ),
            Arguments.of(HttpMethod.GET, "/api/plateforme/serveurs", AuthoritiesConstants.PLATFORM_SUPERVISION_READ),
            Arguments.of(HttpMethod.GET, "/api/plateforme/alertes", AuthoritiesConstants.PLATFORM_SUPERVISION_READ),
            Arguments.of(HttpMethod.GET, "/api/plateforme/flux", AuthoritiesConstants.PLATFORM_SUPERVISION_READ),
            Arguments.of(HttpMethod.GET, "/api/plateforme/configuration", AuthoritiesConstants.PLATFORM_CONFIG_READ),
            Arguments.of(HttpMethod.GET, "/api/plateforme/journalisation/iam", AuthoritiesConstants.PLATFORM_CONFIG_READ),
            Arguments.of(HttpMethod.POST, "/api/plateforme/journalisation/iam", AuthoritiesConstants.PLATFORM_CONFIG_EDIT)
        );
    }

    private WebTestClient.ResponseSpec appeler(WebTestClient c, HttpMethod methode, String chemin) {
        WebTestClient.RequestBodySpec requete = c.mutateWith(csrf()).method(methode).uri(chemin);
        if (methode == HttpMethod.POST) {
            return requete.contentType(MediaType.APPLICATION_JSON).bodyValue(CORPS).exchange();
        }
        return requete.exchange();
    }

    private WebTestClient session(String acr, String... permissions) {
        GrantedAuthority[] autorites = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new);
        return client.mutateWith(
            mockOidcLogin()
                .idToken(t -> {
                    t.subject("super-admin").claim("preferred_username", "super-admin");
                    if (acr != null) {
                        t.claim("acr", acr);
                    }
                })
                .authorities(autorites)
        );
    }

    @ParameterizedTest(name = "{0} {1} : 401 sans session")
    @MethodSource("routes")
    void sansSession(HttpMethod methode, String chemin, String permission) {
        appeler(client, methode, chemin).expectStatus().isUnauthorized();
    }

    @ParameterizedTest(name = "{0} {1} : 403 sans second facteur")
    @MethodSource("routes")
    void sansDoubleFacteur(HttpMethod methode, String chemin, String permission) {
        appeler(session("1", permission), methode, chemin).expectStatus().isForbidden();
        appeler(session(null, permission), methode, chemin).expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0} {1} : 403 sans la permission")
    @MethodSource("routes")
    void sansPermission(HttpMethod methode, String chemin, String permission) {
        appeler(session("2", "ROLE_USER"), methode, chemin).expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0} {1} : 200 avec la permission et le second facteur")
    @MethodSource("routes")
    void autorise(HttpMethod methode, String chemin, String permission) {
        appeler(session("2", permission), methode, chemin).expectStatus().isOk();
        appeler(session("3", permission), methode, chemin).expectStatus().isOk();
    }

    @Test
    @DisplayName("Séparation des pouvoirs : lire la configuration ne permet pas de la modifier ; superviser ne permet pas de la lire")
    void separation() {
        appeler(session("2", AuthoritiesConstants.PLATFORM_CONFIG_READ), HttpMethod.POST, "/api/plateforme/journalisation/iam").expectStatus().isForbidden();
        appeler(session("2", AuthoritiesConstants.PLATFORM_SUPERVISION_READ), HttpMethod.GET, "/api/plateforme/configuration").expectStatus().isForbidden();
        appeler(session("2", AuthoritiesConstants.PLATFORM_CONFIG_EDIT), HttpMethod.GET, "/api/plateforme/services").expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Changement de niveau : l'auteur est l'utilisateur de la session ; motif manquant refusé (400)")
    void auteurEtValidation() {
        appeler(session("2", AuthoritiesConstants.PLATFORM_CONFIG_EDIT), HttpMethod.POST, "/api/plateforme/journalisation/iam")
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.auteur")
            .isEqualTo("super-admin");
        verify(supervision).modifierJournalisation(eq("iam"), eq("cm.gimac.des"), eq("DEBUG"), eq("Analyse de l'incident INC-42"), eq("super-admin"));
        session("2", AuthoritiesConstants.PLATFORM_CONFIG_EDIT)
            .mutateWith(csrf())
            .post()
            .uri("/api/plateforme/journalisation/iam")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("logger", "ROOT", "niveau", "INFO"))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
