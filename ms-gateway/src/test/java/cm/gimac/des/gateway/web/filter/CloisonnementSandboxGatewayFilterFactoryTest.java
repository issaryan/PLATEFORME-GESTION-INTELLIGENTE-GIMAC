package cm.gimac.des.gateway.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.domain.sandbox.JetonReduit;
import cm.gimac.des.gateway.domain.sandbox.PerimetreMandats;
import cm.gimac.des.gateway.domain.sandbox.port.JournalDecisionsSandbox;
import cm.gimac.des.gateway.infrastructure.sandbox.ProjectionPerimetresMemoire;
import cm.gimac.des.gateway.service.sandbox.CloisonnementSandbox;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Filtre CloisonnementSandbox des routes de la sandbox (US-EXI-07), horloge figée au 22/10/2026. */
class CloisonnementSandboxGatewayFilterFactoryTest {

    final List<JournalDecisionsSandbox.Entree> journal = new ArrayList<>();
    final List<JetonReduit> emis = new ArrayList<>();
    final AtomicReference<ServerWebExchange> relaye = new AtomicReference<>();
    ProjectionPerimetresMemoire projection;
    boolean quota;
    GatewayFilter filtre;

    final GatewayFilterChain chaine = ex -> {
        relaye.set(ex);
        return Mono.empty();
    };

    @BeforeEach
    void preparer() {
        projection = new ProjectionPerimetresMemoire();
        quota = true;
        CloisonnementSandbox service = new CloisonnementSandbox(
            projection,
            journal::add,
            Map.of("portail", cle -> Mono.just(quota)),
            j -> {
                emis.add(j);
                return Optional.of("reduit-" + j.institution());
            },
            Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
        );
        filtre = new CloisonnementSandboxGatewayFilterFactory(service).apply(new Object());
    }

    static Authentication jwt(String institution, String... permissions) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u-1").issuedAt(Instant.EPOCH).claim("acr", "2");
        if (institution != null) {
            b.claim("institution_id", institution);
        }
        return new JwtAuthenticationToken(b.build(), java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
    }

    Mono<Void> executer(MockServerWebExchange ex, Authentication auth) {
        Mono<Void> m = filtre.filter(ex, chaine);
        return auth == null ? m : m.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    static MockServerWebExchange echange(MockServerHttpRequest.BaseBuilder<?> requete) {
        return MockServerWebExchange.from(requete.header(HttpHeaders.AUTHORIZATION, "Bearer session").remoteAddress(new InetSocketAddress("10.0.0.9", 4000)));
    }

    @Test
    @DisplayName("Critère 1 : route du portail, jeton de session remplacé par le jeton réduit portant 99001, décision journalisée")
    void routePortailCloisonnee() {
        MockServerWebExchange ex = echange(MockServerHttpRequest.get("/bff/portail/sbx-simulation/essais"));
        executer(ex, jwt("99001", "SBX_SIM_SIMULATION", "NOT_INBOX_READ")).block();
        assertThat(relaye.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer reduit-99001");
        assertThat(emis.get(0).permissions()).containsExactly("SBX_SIM_SIMULATION");
        assertThat(journal).singleElement().satisfies(e -> {
            assertThat(e.ip()).isEqualTo("10.0.0.9");
            assertThat(e.methode()).isEqualTo("GET");
            assertThat(e.chemin()).isEqualTo("/bff/portail/sbx-simulation/essais");
        });
        assertThat(filtre).isInstanceOf(OrderedGatewayFilter.class);
        assertThat(((OrderedGatewayFilter) filtre).getOrder()).isEqualTo(CloisonnementSandboxGatewayFilterFactory.ORDRE);
    }

    @Test
    @DisplayName("Critère 2 : partenaire mandaté, institution mandante 99002 choisie par l'en-tête, en-tête retiré avant le service")
    void partenaireMandate() {
        projection.appliquer(new PerimetreMandats("u-1", "99010", Map.of("99002", Instant.parse("2026-11-10T00:00:00Z")), 1)).block();
        MockServerWebExchange ex = echange(MockServerHttpRequest.post("/bff/portail/sbx-simulation/essais").header(CloisonnementSandboxGatewayFilterFactory.ENTETE_MANDANTE, "99002"));
        executer(ex, jwt("99010", "SBX_SIM_SIMULATION")).block();
        HttpHeaders relayes = relaye.get().getRequest().getHeaders();
        assertThat(relayes.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer reduit-99002");
        assertThat(relayes.containsHeader(CloisonnementSandboxGatewayFilterFactory.ENTETE_MANDANTE)).isFalse();
    }

    @Test
    @DisplayName("Critère 4 : institution hors mandat, 404 RFC 9457 sans révéler la ressource, rien n'est relayé")
    void institutionHorsMandat() {
        projection.appliquer(new PerimetreMandats("u-1", "99010", Map.of("99002", Instant.parse("2026-10-01T00:00:00Z")), 1)).block();
        MockServerWebExchange ex = echange(MockServerHttpRequest.get("/bff/portail/sbx-simulation/essais/e1").header(CloisonnementSandboxGatewayFilterFactory.ENTETE_MANDANTE, "99002"));
        executer(ex, jwt("99010", "SBX_SIM_SIMULATION")).block();
        assertThat(relaye.get()).isNull();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(ex.getResponse().getBodyAsString().block())
            .contains("\"status\":404", "\"code\":\"GW_SBX_INTROUVABLE\"", "Ressource introuvable", "gw-sbx-introuvable")
            .doesNotContain("99002", "mandat");
        assertThat(journal.get(0).decision().motif().name()).isEqualTo("MANDAT_ECHU");
    }

    @Test
    @DisplayName("Quota dépassé : 429, Retry-After, titre dans la langue de l'appelant")
    void quota() {
        quota = false;
        MockServerWebExchange ex = echange(MockServerHttpRequest.get("/bff/portail/sbx-simulation/essais").header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES,es;q=0.9"));
        executer(ex, jwt("99001")).block();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ex.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(ex.getResponse().getBodyAsString().block()).contains("Demasiadas solicitudes", "GW_SBX_QUOTA_DEPASSE");
    }

    @Test
    @DisplayName("Sans authentification, ou chemin hors des routes de la sandbox : 404, rien n'est relayé")
    void refusParDefaut() {
        MockServerWebExchange anonyme = echange(MockServerHttpRequest.get("/bff/portail/sbx-simulation/essais"));
        executer(anonyme, null).block();
        assertThat(anonyme.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        TestingAuthenticationToken nonAuthentifie = new TestingAuthenticationToken("u", "x");
        nonAuthentifie.setAuthenticated(false);
        MockServerWebExchange refuse = echange(MockServerHttpRequest.get("/bff/portail/sbx-simulation/essais"));
        executer(refuse, nonAuthentifie).block();
        assertThat(refuse.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        MockServerWebExchange autre = echange(MockServerHttpRequest.get("/bff/portail/sbx-inconnu/x").header(HttpHeaders.ACCEPT_LANGUAGE, "en"));
        executer(autre, jwt("99001")).block();
        assertThat(autre.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(autre.getResponse().getBodyAsString().block()).contains("Resource not found");
        assertThat(relaye.get()).isNull();
        assertThat(journal).isEmpty();
    }

    @Test
    @DisplayName("Langue du problème : fr par défaut, en et es reconnus, toute autre langue en fr")
    void langue() {
        assertThat(CloisonnementSandboxGatewayFilterFactory.langue(null)).isEqualTo("fr");
        assertThat(CloisonnementSandboxGatewayFilterFactory.langue(" ")).isEqualTo("fr");
        assertThat(CloisonnementSandboxGatewayFilterFactory.langue("en-US,en")).isEqualTo("en");
        assertThat(CloisonnementSandboxGatewayFilterFactory.langue("de")).isEqualTo("fr");
        assertThat(CloisonnementSandboxGatewayFilterFactory.langue("x")).isEqualTo("fr");
    }

    @Test
    @DisplayName("Compte du GIMAC sans adresse distante : console admise sans jeton réduit d'institution")
    void sansAdresse() {
        MockServerWebExchange ex = MockServerWebExchange.from(MockServerHttpRequest.get("/bff/console/sbx-trace/sessions"));
        executer(ex, jwt(null, "SBX_TRA_CONSULTATION")).block();
        assertThat(relaye.get()).isNotNull();
        assertThat(journal.get(0).ip()).isEmpty();
        assertThat(journal.get(0).institutionJeton()).isEmpty();
    }
}
