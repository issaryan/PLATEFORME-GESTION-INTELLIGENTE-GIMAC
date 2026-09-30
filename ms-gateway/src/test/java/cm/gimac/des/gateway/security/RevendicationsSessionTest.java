package cm.gimac.des.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Claims de la session : jeton porteur, jeton d'identité, absence. */
class RevendicationsSessionTest {

    @Test
    @DisplayName("Jeton porteur : sub et institution_id lus ; absents : chaîne vide")
    void jetonPorteur() {
        Jwt jwt = Jwt.withTokenValue("t")
            .header("alg", "none")
            .subject("u1")
            .claim(RevendicationsSession.INSTITUTION, "INST-0042")
            .issuedAt(Instant.now())
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
        assertThat(RevendicationsSession.claim(auth, RevendicationsSession.SUJET)).isEqualTo("u1");
        assertThat(RevendicationsSession.claim(auth, RevendicationsSession.INSTITUTION)).isEqualTo("INST-0042");
        assertThat(RevendicationsSession.claim(auth, "acr")).isEmpty();
    }

    @Test
    @DisplayName("Autre authentification ou aucune : chaîne vide")
    void autre() {
        assertThat(RevendicationsSession.claim(new TestingAuthenticationToken("u1", "x"), RevendicationsSession.SUJET)).isEmpty();
        assertThat(RevendicationsSession.claim(null, RevendicationsSession.SUJET)).isEmpty();
    }
}
