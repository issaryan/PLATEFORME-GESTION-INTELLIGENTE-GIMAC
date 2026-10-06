package cm.gimac.des.gateway.web.rest.plateforme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.config.TestSecurityConfiguration;
import cm.gimac.des.gateway.domain.exploitation.ActeurOperation;
import cm.gimac.des.gateway.domain.exploitation.ChargesProtegees;
import cm.gimac.des.gateway.domain.exploitation.EtatCharge;
import cm.gimac.des.gateway.domain.exploitation.Geste;
import cm.gimac.des.gateway.domain.exploitation.LigneJournal;
import cm.gimac.des.gateway.domain.exploitation.TypeOrchestrateur;
import cm.gimac.des.gateway.domain.supervision.EtatGlobal;
import cm.gimac.des.gateway.domain.supervision.ImpulsionPlateforme;
import cm.gimac.des.gateway.domain.supervision.InstantanePlateforme;
import cm.gimac.des.gateway.infrastructure.exploitation.JournalOperationsMemoire;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import cm.gimac.des.gateway.service.exploitation.ExploitationInfrastructureService;
import cm.gimac.des.gateway.service.exploitation.OrchestrateurSimule;
import cm.gimac.des.gateway.service.supervision.SupervisionPlateformeService;
import cm.gimac.des.gateway.web.filter.OAuth2ReactiveRefreshTokensWebFilter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tech.jhipster.config.JHipsterProperties;

/**
 * Sécurité de /api/plateforme/infrastructure (D-78), sur la vraie chaîne de sécurité de la passerelle et
 * le vrai service d'exploitation (orchestrateur simulé, Kubernetes) : 401 sans session, 403 sans second
 * facteur ou sans la permission ; RSSI (lecture seule) sans gestes ; gestes sans FIDO2 : 401
 * REAUTH_REQUIRED ; avec FIDO2 : 202 ; charge protégée : 409 ; flux temps réel des opérations.
 */
@WebFluxTest(
    controllers = { InfrastructureResource.class, PlateformeResource.class },
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OAuth2ReactiveRefreshTokensWebFilter.class)
)
@Import({ SecurityConfiguration.class, TestSecurityConfiguration.class, InfrastructureResourceSecurityTest.Contexte.class })
class InfrastructureResourceSecurityTest {

    static final String P = "/api/plateforme/infrastructure";
    static final String CORPS = "{\"motif\":\"Analyse de l'incident INC-42\",\"repliques\":2}";
    static final List<String[]> VERIFICATIONS = new CopyOnWriteArrayList<>();

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JHipsterProperties.class)
    static class Contexte {

        @Bean
        OrchestrateurSimule orchestrateurSimule() {
            return new OrchestrateurSimule(TypeOrchestrateur.KUBERNETES);
        }

        @Bean
        ExploitationInfrastructureService exploitationInfrastructureService(OrchestrateurSimule orchestrateur) {
            return new ExploitationInfrastructureService(
                orchestrateur,
                new JournalOperationsMemoire(),
                (assertion, sujet, finalite) -> {
                    VERIFICATIONS.add(new String[] { assertion, sujet, finalite });
                    return Mono.just(!"refusee".equals(assertion));
                },
                op -> {},
                new ExploitationInfrastructureService.Parametres(new ChargesProtegees(ChargesProtegees.DEFAUT), "ADMIN_SENSITIVE", Duration.ofSeconds(2), Duration.ofSeconds(2)),
                Clock.fixed(OrchestrateurSimule.T0, ZoneOffset.UTC)
            );
        }
    }

    @Autowired
    WebTestClient client;

    @Autowired
    OrchestrateurSimule orchestrateur;

    @Autowired
    ExploitationInfrastructureService exploitation;

    @MockitoBean
    SupervisionPlateformeService supervision;

    @BeforeEach
    void preparer() {
        orchestrateur
            .avec("iam", EtatCharge.EN_COURS, 1)
            .avec("audit", EtatCharge.EN_COURS, 1)
            .avec("pilotage", EtatCharge.ARRETEE, 0)
            .avec("notification", EtatCharge.EN_COURS, 1)
            .avec("gateway", EtatCharge.EN_COURS, 1);
        orchestrateur.lignes = List.of(new LigneJournal(Instant.parse("2026-10-05T09:59:00Z"), "stdout", "jeton eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1LTEyMzQ1In0.c2lnbmF0dXJl"));
        InstantanePlateforme instantane = new InstantanePlateforme(OrchestrateurSimule.T0, new EtatGlobal(EtatGlobal.OPERATIONNELLE, 0, 0, 0, 0), List.of(), null);
        when(supervision.flux()).thenReturn(Flux.just(new ImpulsionPlateforme(0, instantane, 0)));
    }

    /** Route, méthode, permission exigée. */
    static Stream<Arguments> routes() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, P + "/charges", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/charges/iam/journaux?lignes=50&depuis=PT5M", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/operations?page=0&taille=10", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/charges/iam/pods", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/charges/iam/evenements", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/noeuds", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.GET, P + "/taches", AuthoritiesConstants.PLATFORM_OPS_READ),
            Arguments.of(HttpMethod.POST, P + "/charges/iam/redemarrage", AuthoritiesConstants.PLATFORM_OPS_EXECUTE),
            Arguments.of(HttpMethod.POST, P + "/charges/audit/arret", AuthoritiesConstants.PLATFORM_OPS_EXECUTE),
            Arguments.of(HttpMethod.POST, P + "/charges/pilotage/demarrage", AuthoritiesConstants.PLATFORM_OPS_EXECUTE),
            Arguments.of(HttpMethod.PUT, P + "/charges/notification/repliques", AuthoritiesConstants.PLATFORM_OPS_EXECUTE)
        );
    }

    static Stream<Arguments> gestes() {
        return routes().filter(a -> a.get()[0] != HttpMethod.GET);
    }

    private WebTestClient session(String acr, String... permissions) {
        GrantedAuthority[] autorites = Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new);
        return client.mutateWith(
            mockOidcLogin()
                .idToken(t -> {
                    t.subject("u-admin-systeme").claim("preferred_username", "admin.systeme").claim("name", "Admin Système");
                    if (acr != null) {
                        t.claim("acr", acr);
                    }
                })
                .authorities(autorites)
        );
    }

    private WebTestClient.ResponseSpec appeler(WebTestClient c, HttpMethod methode, String chemin, String assertion) {
        WebTestClient.RequestBodySpec r = c.mutateWith(csrf()).method(methode).uri(chemin);
        if (assertion != null) {
            r = r.header(InfrastructureResource.REAUTH, assertion);
        }
        if (methode != HttpMethod.GET) {
            return r.contentType(MediaType.APPLICATION_JSON).bodyValue(CORPS).exchange();
        }
        return r.exchange();
    }

    @ParameterizedTest(name = "{0} {1} : 401 sans session")
    @MethodSource("routes")
    void sansSession(HttpMethod methode, String chemin, String permission) {
        appeler(client, methode, chemin, "assertion").expectStatus().isUnauthorized();
    }

    @ParameterizedTest(name = "{0} {1} : 403 sans second facteur")
    @MethodSource("routes")
    void sansDoubleFacteur(HttpMethod methode, String chemin, String permission) {
        appeler(session("1", permission), methode, chemin, "assertion").expectStatus().isForbidden();
        appeler(session(null, permission), methode, chemin, "assertion").expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0} {1} : 403 sans la permission")
    @MethodSource("routes")
    void sansPermission(HttpMethod methode, String chemin, String permission) {
        appeler(session("2", "ROLE_USER", AuthoritiesConstants.PLATFORM_SUPERVISION_READ), methode, chemin, "assertion").expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0} {1} : RSSI (lecture seule) : lecture permise, geste refusé")
    @MethodSource("routes")
    void rssi(HttpMethod methode, String chemin, String permission) {
        WebTestClient rssi = session("2", AuthoritiesConstants.PLATFORM_OPS_READ, AuthoritiesConstants.PLATFORM_SUPERVISION_READ);
        if (methode == HttpMethod.GET) {
            appeler(rssi, methode, chemin, null).expectStatus().isOk();
        } else {
            appeler(rssi, methode, chemin, "assertion").expectStatus().isForbidden();
        }
    }

    @ParameterizedTest(name = "{0} {1} : sans FIDO2, 401 REAUTH_REQUIRED")
    @MethodSource("gestes")
    void sansFido2(HttpMethod methode, String chemin, String permission) {
        appeler(session("2", permission), methode, chemin, null)
            .expectStatus()
            .isUnauthorized()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("REAUTH_REQUIRED");
        appeler(session("2", permission), methode, chemin, "refusee").expectStatus().isUnauthorized().expectBody().jsonPath("$.code").isEqualTo("REAUTH_REQUIRED");
    }

    @ParameterizedTest(name = "{0} {1} : avec FIDO2, 202 et opération EN_COURS")
    @MethodSource("gestes")
    void avecFido2(HttpMethod methode, String chemin, String permission) {
        VERIFICATIONS.clear();
        appeler(session("2", permission), methode, chemin, "YXNzZXJ0aW9u")
            .expectStatus()
            .isAccepted()
            .expectBody()
            .jsonPath("$.resultat")
            .isEqualTo("EN_COURS")
            .jsonPath("$.acteur.id")
            .isEqualTo("u-admin-systeme")
            .jsonPath("$.acteur.nomAffiche")
            .isEqualTo("Admin Système")
            .jsonPath("$.motif")
            .isEqualTo("Analyse de l'incident INC-42");
        assertThat(VERIFICATIONS).singleElement().satisfies(v -> assertThat(v).containsExactly("YXNzZXJ0aW9u", "u-admin-systeme", "ADMIN_SENSITIVE"));
    }

    @Test
    @DisplayName("Lecture : en-tête X-Orchestrateur, gestes permis, protection ; journaux masqués ; paramètres invalides 400")
    void lecture() {
        WebTestClient admin = session("2", AuthoritiesConstants.PLATFORM_OPS_READ);
        admin
            .get()
            .uri(P + "/charges")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .valueEquals("X-Orchestrateur", "KUBERNETES")
            .expectBody()
            .jsonPath("$[?(@.nom=='gateway')].protegee")
            .isEqualTo(true)
            .jsonPath("$[?(@.nom=='gateway')].gestes[0]")
            .isEqualTo("REDEMARRER")
            .jsonPath("$[?(@.nom=='pilotage')].gestes[0]")
            .isEqualTo("DEMARRER");
        admin.get().uri(P + "/charges/iam/journaux").exchange().expectStatus().isOk().expectBody().jsonPath("$.lignes[0].texte").isEqualTo("jeton <jeton masqué>").jsonPath("$.tronque").isEqualTo(false);
        admin.get().uri(P + "/charges/iam/journaux?depuis=10min").exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.code").isEqualTo("OPS_PARAMETRE_INVALIDE");
        admin.get().uri(P + "/charges/iam/journaux?lignes=501").exchange().expectStatus().isBadRequest();
        admin.get().uri(P + "/charges/iam/journaux?depuis=PT31M").exchange().expectStatus().isBadRequest();
        admin.get().uri(P + "/charges/-etc/journaux").exchange().expectStatus().isNotFound().expectBody().jsonPath("$.code").isEqualTo("OPS_CHARGE_INCONNUE");
        admin.get().uri(P + "/operations?taille=500").exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Gestes : charge protégée 409, motif manquant 400, charge inconnue 404, répliques hors bornes 400 ; historique")
    void refus() {
        WebTestClient admin = session("2", AuthoritiesConstants.PLATFORM_OPS_READ, AuthoritiesConstants.PLATFORM_OPS_EXECUTE);
        appeler(admin, HttpMethod.POST, P + "/charges/gateway/arret", "a").expectStatus().isEqualTo(409).expectBody().jsonPath("$.code").isEqualTo("OPS_CHARGE_PROTEGEE");
        admin
            .mutateWith(csrf())
            .post()
            .uri(P + "/charges/iam/redemarrage")
            .header(InfrastructureResource.REAUTH, "a")
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("OPS_MOTIF_INVALIDE");
        appeler(admin, HttpMethod.POST, P + "/charges/inconnue/redemarrage", "a").expectStatus().isNotFound().expectBody().jsonPath("$.code").isEqualTo("OPS_CHARGE_INCONNUE");
        admin
            .mutateWith(csrf())
            .put()
            .uri(P + "/charges/iam/repliques")
            .header(InfrastructureResource.REAUTH, "a")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("motif", "Montée en charge prévue", "repliques", 9))
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("OPS_REPLIQUES_INVALIDES");
        appeler(admin, HttpMethod.POST, P + "/charges/iam/redemarrage", "a").expectStatus().isAccepted();
        admin.get().uri(P + "/operations").exchange().expectStatus().isOk().expectBody().jsonPath("$.total").isNumber().jsonPath("$.contenu[0].charge").isNotEmpty();
    }

    @Test
    @DisplayName("Flux temps réel : opérations poussées (événement operation-infrastructure) à qui porte PLATFORM_OPS_READ")
    void flux() {
        Flux<ServerSentEvent<Map<String, Object>>> evenements = session("2", AuthoritiesConstants.PLATFORM_SUPERVISION_READ, AuthoritiesConstants.PLATFORM_OPS_READ)
            .get()
            .uri("/api/plateforme/flux")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(new ParameterizedTypeReference<ServerSentEvent<Map<String, Object>>>() {})
            .getResponseBody();
        var demande = new ExploitationInfrastructureService.Demande("iam", Geste.REDEMARRER, "Analyse de l'incident INC-42", null, "a", new ActeurOperation("u", "U"));
        StepVerifier.create(evenements)
            .expectNextMatches(e -> "plateforme".equals(e.event()))
            .then(() -> exploitation.executer(demande).subscribe())
            .expectNextMatches(e -> PlateformeResource.EVENEMENT_OPERATION.equals(e.event()) && "EN_COURS".equals(e.data().get("resultat")))
            .expectNextMatches(e -> PlateformeResource.EVENEMENT_OPERATION.equals(e.event()) && "REUSSIE".equals(e.data().get("resultat")))
            .thenCancel()
            .verify(Duration.ofSeconds(10));
        assertThat(PlateformeResource.autorise(null, AuthoritiesConstants.PLATFORM_OPS_READ)).isFalse();
    }
}
