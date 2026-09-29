package cm.gimac.des.document.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** D-61 et RG-59 : décisions d'accès par niveau d'assurance et par institution ; corrélation. */
class AuthorizationManagersTest {

    static JwtAuthenticationToken jeton(String acr, String institution) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("agent");
        if (acr != null) {
            b.claim("acr", acr);
        }
        if (institution != null) {
            b.claim("institution_id", institution);
        }
        return new JwtAuthenticationToken(b.build(), java.util.List.of());
    }

    static boolean fort(boolean exige, Authentication auth) {
        return new StrongAuthenticationAuthorizationManager(exige, Set.of("2", "3")).check(() -> auth, null).isGranted();
    }

    @AfterEach
    void nettoyer() {
        MDC.clear();
    }

    @Test
    @DisplayName("Double facteur : acr 2 ou 3 accepté ; absent ou 1 refusé")
    void doubleFacteur() {
        assertThat(fort(true, jeton("2", null))).isTrue();
        assertThat(fort(true, jeton("3", null))).isTrue();
        assertThat(fort(true, jeton("1", null))).isFalse();
        assertThat(fort(true, jeton(null, null))).isFalse();
    }

    @Test
    @DisplayName("Double facteur non exigé : tout jeton JWT authentifié accepté")
    void nonExige() {
        assertThat(fort(false, jeton(null, null))).isTrue();
    }

    @Test
    @DisplayName("Sans authentification JWT : refus")
    void sansJwt() {
        assertThat(fort(true, null)).isFalse();
        assertThat(fort(false, new TestingAuthenticationToken("u", "p"))).isFalse();
        TestingAuthenticationToken nonAuthentifie = new TestingAuthenticationToken("u", "p");
        nonAuthentifie.setAuthenticated(false);
        assertThat(fort(false, nonAuthentifie)).isFalse();
    }

    @Test
    @DisplayName("Portail : institution obligatoire dans le jeton")
    void portail() {
        InstitutionClaimPresentAuthorizationManager m = new InstitutionClaimPresentAuthorizationManager();
        assertThat(m.check(() -> jeton("2", "inst-1"), null).isGranted()).isTrue();
        assertThat(m.check(() -> jeton("2", " "), null).isGranted()).isFalse();
        assertThat(m.check(() -> jeton("2", null), null).isGranted()).isFalse();
        assertThat(m.check(() -> new TestingAuthenticationToken("u", "p"), null).isGranted()).isFalse();
    }

    @Test
    @DisplayName("Corrélation : en-tête reçu s'il est bien formé, sinon contexte de journalisation, sinon neuf")
    void correlation() {
        assertThat(CorrelationContext.depuis("abc-123")).isEqualTo("abc-123");
        String neuf = CorrelationContext.depuis("mal formé <script>");
        assertThat(neuf).hasSize(36);
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isEqualTo(neuf);
        assertThat(CorrelationContext.depuis(null)).isEqualTo(neuf);
        MDC.put(CorrelationContext.MDC_KEY, " ");
        assertThat(CorrelationContext.depuis(null)).isNotBlank().isNotEqualTo(neuf);
    }
}
