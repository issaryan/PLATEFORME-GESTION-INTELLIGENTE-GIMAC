package cm.gimac.des.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.security.session.ParametresSession;
import cm.gimac.des.gateway.security.session.SessionGimac;
import java.time.Duration;
import java.time.Instant;
import org.springframework.mock.web.server.MockWebSession;
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
        SessionResource.SessionVue s = resource.session(auth, null).block();
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
        SessionResource.SessionVue s = resource.session(new JwtAuthenticationToken(jwt, List.of()), null).block();
        assertThat(s.doubleFacteur()).isFalse();
        assertThat(s.nom()).isEqualTo("agent");
        assertThat(s.institutionId()).isEmpty();
    }

    @Test
    @DisplayName("Autre authentification : aucun claim")
    void autre() {
        SessionResource.SessionVue s = resource.session(new TestingAuthenticationToken("x", "y", "ROLE_USER"), new MockWebSession()).block();
        assertThat(s.acr()).isEmpty();
        assertThat(s.roles()).containsExactly("ROLE_USER");
        assertThat(s.doubleFacteur()).isFalse();
        assertThat(s.expireA()).isNull();
        assertThat(s.restreinte()).isFalse();
    }

    @Test
    @DisplayName("Session de la plateforme : fin d'inactivité prévue, bornée par la durée maximale ; état restreint")
    void echeance() {
        Instant t0 = Instant.parse("2026-09-30T08:00:00Z");
        var horloge = new cm.gimac.des.gateway.test.util.HorlogeReglable(t0);
        var r = new SessionResource(Set.of("2", "3"), new ParametresSession(Duration.ofMinutes(30), Duration.ofHours(10)), horloge);
        MockWebSession session = new MockWebSession();
        session.getAttributes().put(SessionGimac.DEBUT, t0);
        session.getAttributes().put(SessionGimac.DERNIERE_ACTIVITE, t0.plus(Duration.ofMinutes(10)));
        session.getAttributes().put(SessionGimac.RESTREINTE, true);
        SessionResource.SessionVue s = r.session(new TestingAuthenticationToken("x", "y"), session).block();
        assertThat(s.expireA()).isEqualTo("2026-09-30T08:40:00Z");
        assertThat(s.restreinte()).isTrue();
        session.getAttributes().put(SessionGimac.DERNIERE_ACTIVITE, t0.plus(Duration.ofHours(9).plusMinutes(50)));
        assertThat(r.session(new TestingAuthenticationToken("x", "y"), session).block().expireA()).isEqualTo("2026-09-30T18:00:00Z");
        horloge.avancer(Duration.ofHours(1));
        assertThat(r.prolonger(session).block().getStatusCode().value()).isEqualTo(204);
        assertThat((Instant) session.getAttribute(SessionGimac.DERNIERE_ACTIVITE)).isEqualTo(t0.plus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("Prolongation hors session de la plateforme : 204 sans effet")
    void prolongationSansSession() {
        MockWebSession session = new MockWebSession();
        assertThat(resource.prolonger(session).block().getStatusCode().value()).isEqualTo(204);
        assertThat(session.getAttributes()).isEmpty();
    }
}
