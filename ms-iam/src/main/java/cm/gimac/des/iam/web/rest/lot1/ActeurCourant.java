package cm.gimac.des.iam.web.rest.lot1;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Acteur authentifié, lu dans le jeton : sujet, nom affiché et permissions. */
final class ActeurCourant {

    private ActeurCourant() {}

    static Acteur de(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt) || jwt.getToken().getSubject() == null || jwt.getToken().getSubject().isBlank()) {
            throw IamException.interdit("IAM_USER_TOKEN_REQUIRED", "Jeton utilisateur exigé");
        }
        Jwt t = jwt.getToken();
        String nom = t.getClaimAsString("name");
        if (nom == null || nom.isBlank()) {
            nom = t.getClaimAsString("preferred_username");
        }
        return new Acteur(t.getSubject(), nom, auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
    }
}
