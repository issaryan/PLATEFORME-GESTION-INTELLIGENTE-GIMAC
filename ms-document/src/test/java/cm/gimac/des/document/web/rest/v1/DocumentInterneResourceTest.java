package cm.gimac.des.document.web.rest.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.document.domain.DocumentException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Logique propre à l'interface interne : client du service appelant et en-tête Repr-Digest. */
class DocumentInterneResourceTest {

    static JwtAuthenticationToken jeton(String azp, String nom) {
        return new JwtAuthenticationToken(
            Jwt.withTokenValue("t").header("alg", "none").subject("s").claim("azp", azp).claim("preferred_username", nom).build(),
            List.of()
        );
    }

    @Test
    @DisplayName("Client du service lu dans le jeton de service")
    void service() {
        assertThat(DocumentInterneResource.service(jeton("collaboration-service", "service-account-collaboration-service"))).isEqualTo("collaboration-service");
    }

    @Test
    @DisplayName("Jeton utilisateur ou authentification sans JWT : 403 DOC_SERVICE_TOKEN_REQUIRED")
    void jetonDeServiceExige() {
        assertThatThrownBy(() -> DocumentInterneResource.service(jeton("web_app", "agent.ddrm"))).isInstanceOfSatisfying(DocumentException.class, e -> {
            assertThat(e.code()).isEqualTo("DOC_SERVICE_TOKEN_REQUIRED");
            assertThat(e.status()).isEqualTo(403);
        });
        assertThatThrownBy(() -> DocumentInterneResource.service(new TestingAuthenticationToken("u", "p"))).hasFieldOrPropertyWithValue(
            "code",
            "DOC_SERVICE_TOKEN_REQUIRED"
        );
        assertThatThrownBy(() -> DocumentInterneResource.service(null)).hasFieldOrPropertyWithValue("code", "DOC_SERVICE_TOKEN_REQUIRED");
    }

    @Test
    @DisplayName("Content-Disposition : pièce jointe, nom UTF-8 encodé selon RFC 8187")
    void pieceJointe() {
        assertThat(DocumentInterneResource.pieceJointe("a-Z_0.9!#$&+^`|~.txt")).isEqualTo("attachment; filename*=UTF-8''a-Z_0.9!#$&+^`|~.txt");
        assertThat(DocumentInterneResource.pieceJointe("procès \"v\";n°2.pdf")).isEqualTo(
            "attachment; filename*=UTF-8''proc%C3%A8s%20%22v%22%3Bn%C2%B02.pdf"
        );
    }

    @Test
    @DisplayName("Repr-Digest (RFC 9530) : empreinte hexadécimale réencodée en base64")
    void reprDigest() {
        // SHA-256 de la chaîne vide.
        assertThat(DocumentInterneResource.reprDigest("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")).isEqualTo(
            "sha-256=:47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=:"
        );
    }
}
