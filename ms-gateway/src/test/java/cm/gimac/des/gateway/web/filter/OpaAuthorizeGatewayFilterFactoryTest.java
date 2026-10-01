package cm.gimac.des.gateway.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/**
 * Filtre OpaAuthorize (PI §0.6, D-61) : entrée soumise à OPA (permissions, institution, niveau
 * d'assurance acr), décision appliquée, refus si OPA est injoignable. OPA est simulé par un
 * serveur HTTP local qui renvoie la décision voulue et mémorise la requête reçue.
 */
class OpaAuthorizeGatewayFilterFactoryTest {

    HttpServer opa;
    final AtomicReference<String> recu = new AtomicReference<>();
    volatile String decision = "{\"result\":true}";
    OpaAuthorizeGatewayFilterFactory factory;

    @BeforeEach
    void demarrer() throws IOException {
        opa = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        opa.createContext("/v1/data/gimac/authz/allow", ex -> {
            recu.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] corps = decision.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, corps.length);
            ex.getResponseBody().write(corps);
            ex.close();
        });
        opa.start();
        factory = new OpaAuthorizeGatewayFilterFactory("http://127.0.0.1:" + opa.getAddress().getPort());
    }

    @AfterEach
    void arreter() {
        opa.stop(0);
    }

    static Authentication jwt(String acr, String institution, String... permissions) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u").issuedAt(Instant.EPOCH);
        if (acr != null) {
            b.claim("acr", acr);
        }
        if (institution != null) {
            b.claim("institution_id", institution);
        }
        return new JwtAuthenticationToken(b.build(), java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
    }

    static MockServerWebExchange echange(String chemin) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(chemin));
    }

    private boolean filtrer(GatewayFilter filtre, MockServerWebExchange echange, Authentication auth) {
        boolean[] transmis = { false };
        Mono<Void> m = filtre.filter(echange, e -> {
            transmis[0] = true;
            return Mono.empty();
        });
        if (auth != null) {
            m = m.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        }
        m.block();
        return transmis[0];
    }

    @Test
    @DisplayName("Entrée OPA : méthode, chemin, permissions, institution et acr du jeton porteur")
    void entreeJeton() {
        Map<String, Object> entree = factory.entree(echange("/bff/des/participant/institutions"), jwt("2", "INST-0042", "PAR_INSTITUTION_CREATE"));
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) entree.get("input");
        assertThat(input)
            .containsEntry("method", "POST")
            .containsEntry("path", "/bff/des/participant/institutions")
            .containsEntry("permissions", List.of("PAR_INSTITUTION_CREATE"))
            .containsEntry("institution_id", "INST-0042")
            .containsEntry("acr", "2")
            .containsEntry("authentifie", true);
    }

    @Test
    @DisplayName("Session de la passerelle : claims lus dans le jeton d'identité")
    void entreeSessionOidc() {
        OidcIdToken id = new OidcIdToken("t", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), Map.of("sub", "u", "acr", "2"));
        var auth = new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), id), List.of(new SimpleGrantedAuthority("PAR_INSTITUTION_READ")), "oidc");
        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) factory.entree(echange("/bff/des/x"), auth).get("input");
        assertThat(input).containsEntry("acr", "2").containsEntry("institution_id", "");
    }

    @Test
    @DisplayName("Chemin d'origine conservé après réécriture")
    void cheminOrigine() {
        MockServerWebExchange e = echange("/api/v1/institutions");
        LinkedHashSet<URI> origines = new LinkedHashSet<>(List.of(URI.create("http://gw/bff/des/participant/institutions")));
        e.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR, origines);
        assertThat(OpaAuthorizeGatewayFilterFactory.cheminOrigine(e)).isEqualTo("/bff/des/participant/institutions");
        assertThat(OpaAuthorizeGatewayFilterFactory.cheminOrigine(echange("/bff/des/y"))).isEqualTo("/bff/des/y");
    }

    @Test
    @DisplayName("Décision favorable : la requête est transmise ; l'entrée reçue par OPA porte acr")
    void autorise() {
        MockServerWebExchange e = echange("/bff/des/participant/institutions");
        assertThat(filtrer(factory.apply(new Object()), e, jwt("2", null, "PAR_INSTITUTION_CREATE"))).isTrue();
        assertThat(recu.get()).contains("\"acr\":\"2\"").contains("PAR_INSTITUTION_CREATE");
    }

    @Test
    @DisplayName("Décision défavorable : 403, la requête n'est pas transmise")
    void refuse() {
        decision = "{\"result\":false}";
        MockServerWebExchange e = echange("/bff/des/participant/institutions");
        assertThat(filtrer(factory.apply(new Object()), e, jwt("1", null, "PAR_INSTITUTION_CREATE"))).isFalse();
        assertThat(e.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Anonyme : entrée non authentifiée, sans permission ni acr")
    void anonyme() {
        decision = "{}";
        MockServerWebExchange e = echange("/bff/des/participant/institutions");
        assertThat(filtrer(factory.apply(new Object()), e, null)).isFalse();
        assertThat(recu.get()).contains("\"authentifie\":false").contains("\"acr\":\"\"");
    }

    @Test
    @DisplayName("Session restreinte du premier accès (D-65) : « restreinte » transmis à OPA")
    void restreinte() {
        org.springframework.mock.web.server.MockWebSession session = new org.springframework.mock.web.server.MockWebSession();
        session.getAttributes().put(cm.gimac.des.gateway.security.session.SessionGimac.RESTREINTE, true);
        MockServerWebExchange e = MockServerWebExchange.builder(MockServerHttpRequest.get("/bff/des/iam/moi")).session(session).build();
        assertThat(filtrer(factory.apply(new Object()), e, jwt("1", null))).isTrue();
        assertThat(recu.get()).contains("\"restreinte\":true").contains("\"acr\":\"1\"");
        MockServerWebExchange sans = echange("/bff/des/iam/moi");
        filtrer(factory.apply(new Object()), sans, jwt("2", null));
        assertThat(recu.get()).contains("\"restreinte\":false");
    }

    @Test
    @DisplayName("Authentification anonyme : sans principal ni justificatif")
    void marqueurAnonyme() {
        decision = "{\"result\":true}";
        MockServerWebExchange e = echange("/bff/des/x");
        filtrer(factory.apply(new Object()), e, null);
        assertThat(recu.get()).contains("\"authentifie\":false");
        var m = new OpaAuthorizeGatewayFilterFactory.AnonymousMarker();
        assertThat(m.getPrincipal()).isEqualTo("anonyme");
        assertThat(m.getCredentials()).isEqualTo("");
    }

    @Test
    @DisplayName("OPA injoignable : refus par défaut (RG-24)")
    void opaInjoignable() {
        opa.stop(0);
        MockServerWebExchange e = echange("/bff/des/participant/institutions");
        assertThat(filtrer(factory.apply(new Object()), e, jwt("2", null, "PAR_INSTITUTION_CREATE"))).isFalse();
        assertThat(e.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
