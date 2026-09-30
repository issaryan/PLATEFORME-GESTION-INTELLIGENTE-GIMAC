package cm.gimac.des.certification.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.domain.event.DecisionEvenements;
import cm.gimac.des.certification.infrastructure.messaging.TopicResolver;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** D-61 et RG-59 : second facteur (acr) sur les routes métier ; institution du jeton au portail. Sujets des événements. */
class StrongAuthenticationAuthorizationManagerTest {

    static JwtAuthenticationToken jeton(String acr, String institution) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u").issuedAt(Instant.now());
        if (acr != null) {
            b.claim("acr", acr);
        }
        if (institution != null) {
            b.claim("institution_id", institution);
        }
        return new JwtAuthenticationToken(b.build(), List.of(new SimpleGrantedAuthority("CER_DOSSIER_READ")));
    }

    static boolean accorde(StrongAuthenticationAuthorizationManager m, org.springframework.security.core.Authentication a) {
        return m.authorize(() -> a, null).isGranted();
    }

    @Test
    @DisplayName("Routes métier : acr 2 ou 3 exigé ; exigence désactivable ; autre authentification refusée")
    void routesMetier() {
        var m = new StrongAuthenticationAuthorizationManager(true, Set.of("2", "3"));
        assertThat(accorde(m, jeton("2", null))).isTrue();
        assertThat(accorde(m, jeton("3", null))).isTrue();
        assertThat(accorde(m, jeton("1", null))).isFalse();
        assertThat(accorde(m, jeton(null, null))).isFalse();
        assertThat(accorde(m, null)).isFalse();
        assertThat(accorde(m, new TestingAuthenticationToken("u", "p", "CER_DOSSIER_READ"))).isFalse();
        assertThat(accorde(new StrongAuthenticationAuthorizationManager(false, Set.of()), jeton(null, null))).isTrue();
    }

    @Test
    @DisplayName("Portail : institution exigée en plus du second facteur")
    void portail() {
        var m = new StrongAuthenticationAuthorizationManager(true, Set.of("2"), true);
        assertThat(accorde(m, jeton("2", "INST-0042"))).isTrue();
        assertThat(accorde(m, jeton("2", " "))).isFalse();
        assertThat(accorde(m, jeton("2", null))).isFalse();
        assertThat(accorde(m, jeton("1", "INST-0042"))).isFalse();
    }

    @Test
    @DisplayName("Sujets : certification.proces-verbal et certification.approbation ; règle générale ailleurs")
    void sujets() {
        TopicResolver r = new TopicResolver();
        assertThat(r.topicFor(DecisionEvenements.PV_ETABLI)).isEqualTo("certification.proces-verbal");
        assertThat(r.topicFor(DecisionEvenements.PV_SIGNE)).isEqualTo("certification.proces-verbal");
        assertThat(r.topicFor(DecisionEvenements.SECURITE_APPROUVEE)).isEqualTo("certification.approbation");
        assertThat(r.topicFor(DecisionEvenements.DOSSIER_CHANGEMENT_CONSTITUE)).isEqualTo("certification.approbation");
        assertThat(r.topicFor("certification.quitus-signed.v1")).isEqualTo("certification.quitus");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> r.topicFor("mal.forme")).isInstanceOf(IllegalArgumentException.class);
    }
}
