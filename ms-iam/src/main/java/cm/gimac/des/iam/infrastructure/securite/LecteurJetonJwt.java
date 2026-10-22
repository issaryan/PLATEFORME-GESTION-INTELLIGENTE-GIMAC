package cm.gimac.des.iam.infrastructure.securite;

import cm.gimac.des.iam.application.port.out.sandbox.LecteurJeton;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Lecture d'un jeton présenté à l'introspection de la sandbox (D-119) par le décodeur des jetons de MS-IAM : signature
 * du fournisseur d'identité du GIMAC, émetteur, audience et échéance ; tout défaut rend un jeton invalide.
 */
@Component
public class LecteurJetonJwt implements LecteurJeton {

    private final JwtDecoder decodeur;

    public LecteurJetonJwt(JwtDecoder decodeur) {
        this.decodeur = decodeur;
    }

    @Override
    public Optional<Map<String, Object>> lire(String jeton) {
        if (jeton == null || jeton.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(decodeur.decode(jeton.strip()).getClaims());
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
