package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Session exposée aux couches de présentation (D-58, D-61). */
class SessionResourceTest {

    final SessionResource resource = new SessionResource(Set.of("2", "3"));

    @Test
    @DisplayName("Session de la passerelle : rôles, permissions triées, institution et second facteur")
    void sessionOidc() {
        OidcIdToken id = new OidcIdToken(
            "t",
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(60),
            Map.of("sub", "u-1", "name", "Chef DDRM", "preferred_username", "test-chef-ddrm", "acr", "2", "institution_id", "INST-0042")
        );
        var auth = new OAuth2AuthenticationToken(
            new DefaultOidcUser(List.of(), id),
            List.of(
                new SimpleGrantedAuthority("PAR_TRANSITION_APPROVE"),
                new SimpleGrantedAuthority("ROLE_CHEF_DDRM"),
                new SimpleGrantedAuthority("PAR_INSTITUTION_READ")
            ),
            "oidc"
        );
        SessionResource.SessionVue s = resource.session(auth).block();
        assertThat(s.sujet()).isEqualTo("u-1");
        assertThat(s.nom()).isEqualTo("Chef DDRM");
        assertThat(s.identifiant()).isEqualTo("test-chef-ddrm");
        assertThat(s.roles()).containsExactly("ROLE_CHEF_DDRM");
        assertThat(s.permissions()).containsExactly("PAR_INSTITUTION_READ", "PAR_TRANSITION_APPROVE");
        assertThat(s.institutionId()).isEqualTo("INST-0042");
        assertThat(s.doubleFacteur()).isTrue();
    }

    @Test
    @DisplayName("Jeton porteur de niveau 1 : second facteur absent ; nom repris de l'identifiant")
    void jetonNiveau1() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u-2").claim("acr", "1").claim("preferred_username", "agent").build();
        SessionResource.SessionVue s = resource.session(new JwtAuthenticationToken(jwt, List.of())).block();
        assertThat(s.doubleFacteur()).isFalse();
        assertThat(s.nom()).isEqualTo("agent");
        assertThat(s.institutionId()).isEmpty();
    }

    @Test
    @DisplayName("Autre authentification : aucun claim")
    void autre() {
        SessionResource.SessionVue s = resource.session(new TestingAuthenticationToken("x", "y", "ROLE_USER")).block();
        assertThat(s.acr()).isEmpty();
        assertThat(s.roles()).containsExactly("ROLE_USER");
        assertThat(s.doubleFacteur()).isFalse();
    }
}
