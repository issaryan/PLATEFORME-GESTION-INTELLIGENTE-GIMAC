package cm.gimac.des.gateway.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.config.SecurityConfiguration;
import cm.gimac.des.gateway.security.AuthoritiesConstants;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;

/**
 * Authentification d'un jeton porteur : le refus (403) ou l'indisponibilité de userinfo ne fait
 * jamais échouer l'authentification ; un jeton de compte de service n'appelle pas userinfo et
 * garde les autorités de ses claims.
 */
class EnrichissementJetonTest {

    private final AtomicInteger appels = new AtomicInteger();
    private final Cache<String, Mono<Jwt>> cache = Caffeine.newBuilder().build();

    private EnrichissementJeton enrichissement(Mono<Map<String, Object>> reponse, Duration delai) {
        return new EnrichissementJeton(
            jeton -> {
                appels.incrementAndGet();
                return reponse;
            },
            cache,
            delai
        );
    }

    static Jwt jeton(Map<String, Object> claims) {
        Map<String, Object> c = new HashMap<>(claims);
        c.putIfAbsent("iat", Instant.now());
        c.putIfAbsent("exp", Instant.now().plusSeconds(300));
        return Jwt.withTokenValue("jeton").header("alg", "RS256").claims(m -> m.putAll(c)).build();
    }

    /** Jeton client_credentials de « gateway-supervision », tel que l'émet le domaine gimac. */
    static Jwt jetonSupervision() {
        return jeton(
            Map.of(
                "sub",
                "b1c2d3",
                "azp",
                "gateway-supervision",
                "preferred_username",
                "service-account-gateway-supervision",
                "roles",
                List.of("default-roles-gimac", "offline_access", AuthoritiesConstants.ADMIN),
                "scope",
                "profile email"
            )
        );
    }

    static WebClientResponseException interdit() {
        return WebClientResponseException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], null, null);
    }

    @Test
    void compteDeServiceSansAppelUserinfoEtAutoritesDesClaims() {
        Jwt jwt = jetonSupervision();

        Jwt resultat = enrichissement(Mono.error(interdit()), Duration.ofSeconds(1)).enrichir(jwt).block();

        assertThat(resultat).isSameAs(jwt);
        assertThat(appels).hasValue(0);
        AbstractAuthenticationToken authentification = new SecurityConfiguration(new JHipsterProperties())
            .jwtAuthenticationConverter()
            .convert(resultat)
            .block();
        assertThat(authentification.getName()).isEqualTo("service-account-gateway-supervision");
        assertThat(authentification.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(AuthoritiesConstants.ADMIN);
    }

    @Test
    void reconnaitLesComptesDeService() {
        assertThat(EnrichissementJeton.compteDeService(jetonSupervision())).isTrue();
        assertThat(EnrichissementJeton.compteDeService(jeton(Map.of("sub", "x", "client_id", "gateway-supervision")))).isTrue();
        assertThat(EnrichissementJeton.compteDeService(jeton(Map.of("azp", "gateway-supervision")))).isTrue();
        assertThat(EnrichissementJeton.compteDeService(jeton(Map.of("sub", "u-1", "preferred_username", "agent.sna")))).isFalse();
    }

    @Test
    void utilisateurUserinfoRefuseRepliSurLesClaims() {
        Jwt jwt = jeton(Map.of("sub", "u-1", "preferred_username", "agent.sna", "roles", List.of("ROLE_SNA")));

        Jwt resultat = enrichissement(Mono.error(interdit()), Duration.ofSeconds(1)).enrichir(jwt).block();

        assertThat(resultat).isSameAs(jwt);
        assertThat(appels).hasValue(1);
        // Le repli n'est pas mis en cache : userinfo est de nouveau tenté à la requête suivante.
        assertThat(cache.getIfPresent("u-1")).isNull();
    }

    @Test
    void utilisateurUserinfoTropLentRepliSurLesClaims() {
        Jwt jwt = jeton(Map.of("sub", "u-1", "preferred_username", "agent.sna"));

        Jwt resultat = enrichissement(Mono.never(), Duration.ofMillis(50)).enrichir(jwt).block(Duration.ofSeconds(5));

        assertThat(resultat).isSameAs(jwt);
    }

    @Test
    void utilisateurEnrichiParUserinfoLesClaimsDuJetonPriment() {
        Jwt jwt = jeton(Map.of("sub", "u-1", "preferred_username", "agent.sna", "roles", List.of("ROLE_SNA")));
        Map<String, Object> reponse = new HashMap<>(Map.of("sub", "u-1", "name", "Awa Ndiaye Mbarga", "roles", List.of("ROLE_ADMIN")));

        EnrichissementJeton e = enrichissement(Mono.just(reponse), Duration.ofSeconds(1));
        Jwt resultat = e.enrichir(jwt).block();

        assertThat(resultat.getClaimAsString("given_name")).isEqualTo("Awa");
        assertThat(resultat.getClaimAsString("family_name")).isEqualTo("Ndiaye Mbarga");
        assertThat(resultat.getClaimAsStringList("roles")).containsExactly("ROLE_SNA");
        // Seconde requête servie par le cache.
        assertThat(e.enrichir(jwt).block().getClaimAsString("given_name")).isEqualTo("Awa");
        assertThat(appels).hasValue(1);
    }

    @Test
    void identiteDejaPresenteSansAppel() {
        Jwt jwt = jeton(Map.of("sub", "u-1", "given_name", "Awa", "family_name", "Ndiaye"));

        assertThat(enrichissement(Mono.error(interdit()), Duration.ofSeconds(1)).enrichir(jwt).block()).isSameAs(jwt);
        assertThat(appels).hasValue(0);
    }
}
