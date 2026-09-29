package cm.gimac.des.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

/** Accès à deux facteurs (D-61) : acr lu dans la session OIDC ou le jeton porteur. */
class DoubleFacteurAuthorizationManagerTest {

    final DoubleFacteurAuthorizationManager gestionnaire = new DoubleFacteurAuthorizationManager(Set.of("2", "3"));

    boolean accorde(Authentication auth) {
        return gestionnaire.authorize(auth == null ? Mono.empty() : Mono.just(auth), null).block().isGranted();
    }

    static Authentication session(String acr) {
        Map<String, Object> claims = acr == null ? Map.of("sub", "u") : Map.of("sub", "u", "acr", acr);
        OidcIdToken id = new OidcIdToken("t", Instant.EPOCH, Instant.EPOCH.plusSeconds(60), claims);
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), id), List.of(new SimpleGrantedAuthority("PLATFORM_SUPERVISION_READ")), "oidc");
    }

    @Test
    @DisplayName("Session OIDC : acr 2 ou 3 accordé, 1 ou absent refusé")
    void sessionOidc() {
        assertThat(accorde(session("2"))).isTrue();
        assertThat(accorde(session("3"))).isTrue();
        assertThat(accorde(session("1"))).isFalse();
        assertThat(accorde(session(null))).isFalse();
    }

    @Test
    @DisplayName("Jeton porteur : acr lu dans les claims")
    void jetonPorteur() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u").claim("acr", "2").build();
        assertThat(accorde(new JwtAuthenticationToken(jwt, List.of()))).isTrue();
    }

    @Test
    @DisplayName("Sans authentification, anonyme ou non authentifié : refusé")
    void sansAuthentification() {
        assertThat(accorde(null)).isFalse();
        assertThat(
            accorde(new AnonymousAuthenticationToken("cle", "anonyme", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))))
        ).isFalse();
        TestingAuthenticationToken nonAuthentifie = new TestingAuthenticationToken("u", "p");
        nonAuthentifie.setAuthenticated(false);
        assertThat(accorde(nonAuthentifie)).isFalse();
        assertThat(DoubleFacteurAuthorizationManager.acr(new TestingAuthenticationToken("u", "p"))).isEmpty();
    }
}
