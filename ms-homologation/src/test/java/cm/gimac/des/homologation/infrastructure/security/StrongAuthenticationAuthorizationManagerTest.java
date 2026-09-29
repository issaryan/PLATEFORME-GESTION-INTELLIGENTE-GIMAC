package cm.gimac.des.homologation.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** D-61 : décision du gestionnaire d'autorisation à deux facteurs. */
class StrongAuthenticationAuthorizationManagerTest {

    static Authentication jwt(String acr) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("s").issuedAt(Instant.EPOCH);
        if (acr != null) {
            b.claim("acr", acr);
        }
        return new JwtAuthenticationToken(b.build(), java.util.List.of());
    }

    static boolean decide(StrongAuthenticationAuthorizationManager m, Authentication a) {
        return m.authorize(() -> a, null).isGranted();
    }

    @Test
    @DisplayName("Exigé : seuls les niveaux acceptés passent")
    void exige() {
        var m = new StrongAuthenticationAuthorizationManager(true, Set.of("2", "3"));
        assertThat(decide(m, jwt("2"))).isTrue();
        assertThat(decide(m, jwt("3"))).isTrue();
        assertThat(decide(m, jwt("1"))).isFalse();
        assertThat(decide(m, jwt(null))).isFalse();
    }

    @Test
    @DisplayName("Non exigé : tout jeton utilisateur authentifié passe ; jamais un autre type d'authentification")
    void nonExige() {
        var m = new StrongAuthenticationAuthorizationManager(false, Set.of());
        assertThat(decide(m, jwt(null))).isTrue();
        assertThat(decide(m, new TestingAuthenticationToken("u", "p", "ROLE_USER"))).isFalse();
        assertThat(decide(m, null)).isFalse();
        Authentication nonAuthentifie = jwt("2");
        nonAuthentifie.setAuthenticated(false);
        assertThat(decide(m, nonAuthentifie)).isFalse();
    }
}
