package cm.gimac.des.iam.infrastructure.securite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lecture d'un jeton présenté à l'introspection de la sandbox (D-119), sans Docker. */
class LecteurJetonJwtTest {

    @Test
    @DisplayName("Jeton décodé : ses revendications ; jeton vide ou refusé par le décodeur : vide")
    void lecture() {
        LecteurJetonJwt lecteur = new LecteurJetonJwt(j -> {
            if (!j.equals("bon")) {
                throw new BadJwtException("signature");
            }
            return Jwt.withTokenValue(j).header("alg", "RS256").subject("s1").issuedAt(Instant.EPOCH).expiresAt(Instant.EPOCH.plusSeconds(60)).build();
        });
        assertThat(lecteur.lire(" bon ")).hasValueSatisfying(c -> assertThat(c).containsEntry("sub", "s1"));
        assertThat(lecteur.lire("mauvais")).isEmpty();
        assertThat(lecteur.lire(" ")).isEmpty();
        assertThat(lecteur.lire(null)).isEmpty();
    }
}
