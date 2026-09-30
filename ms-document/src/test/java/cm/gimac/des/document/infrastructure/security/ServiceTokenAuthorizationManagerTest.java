package cm.gimac.des.document.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Interfaces internes : jeton de service (compte de service du client appelant) et permission interne de la route. */
class ServiceTokenAuthorizationManagerTest {

    static Jwt jwt(String azp, String clientId, String nom) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("sujet");
        if (azp != null) {
            b.claim("azp", azp);
        }
        if (clientId != null) {
            b.claim("client_id", clientId);
        }
        if (nom != null) {
            b.claim("preferred_username", nom);
        }
        return b.build();
    }

    static boolean accorde(Authentication auth) {
        return new ServiceTokenAuthorizationManager("INTERNAL_DOCUMENT_DEPOSIT").check(() -> auth, null).isGranted();
    }

    static JwtAuthenticationToken jeton(Jwt jwt, String... permissions) {
        return new JwtAuthenticationToken(jwt, java.util.Arrays.stream(permissions).<org.springframework.security.core.GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    @DisplayName("Client lu dans azp, à défaut dans client_id, si le nom est celui de son compte de service")
    void clientDeService() {
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt("collaboration-service", null, "service-account-collaboration-service"))).contains(
            "collaboration-service"
        );
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt(" ", "collaboration-service", "SERVICE-ACCOUNT-collaboration-service"))).contains(
            "collaboration-service"
        );
    }

    @Test
    @DisplayName("Jeton utilisateur ou jeton incomplet : pas de client de service")
    void pasUnJetonDeService() {
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt("web_app", null, "agent.ddrm"))).isEmpty();
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt("web_app", null, "service-account-collaboration-service"))).isEmpty();
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt(null, null, "service-account-collaboration-service"))).isEmpty();
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt(null, " ", "service-account-"))).isEmpty();
        assertThat(ServiceTokenAuthorizationManager.clientDeService(jwt("collaboration-service", null, null))).isEmpty();
    }

    @Test
    @DisplayName("Accès : jeton de service porteur de la permission de la route ; refus sinon")
    void decision() {
        Jwt service = jwt("collaboration-service", null, "service-account-collaboration-service");
        assertThat(accorde(jeton(service, "INTERNAL_DOCUMENT_DEPOSIT"))).isTrue();
        assertThat(accorde(jeton(service, "INTERNAL_DOCUMENT_READ"))).isFalse();
        assertThat(accorde(jeton(jwt("web_app", null, "agent"), "INTERNAL_DOCUMENT_DEPOSIT"))).isFalse();
    }

    @Test
    @DisplayName("Sans authentification JWT : refus")
    void sansJwt() {
        assertThat(accorde(null)).isFalse();
        assertThat(accorde(new TestingAuthenticationToken("u", "p", List.of(new SimpleGrantedAuthority("INTERNAL_DOCUMENT_DEPOSIT"))))).isFalse();
        JwtAuthenticationToken nonAuthentifie = jeton(jwt("collaboration-service", null, "service-account-collaboration-service"), "INTERNAL_DOCUMENT_DEPOSIT");
        nonAuthentifie.setAuthenticated(false);
        assertThat(accorde(nonAuthentifie)).isFalse();
    }
}
