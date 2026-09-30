package cm.gimac.des.gateway.security;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Claims de l'utilisateur authentifié, lus dans le jeton d'identité de la session OIDC de la
 * passerelle (connexion par le navigateur) ou dans le jeton porteur.
 */
public final class RevendicationsSession {

    /** Identifiant du compte (MS-IAM), destinataire des messages de la messagerie. */
    public static final String SUJET = "sub";
    /** Institution du participant (RG-59), absente pour un acteur interne. */
    public static final String INSTITUTION = "institution_id";

    private RevendicationsSession() {}

    /** Valeur textuelle du claim, chaîne vide s'il est absent ou sans authentification. */
    public static String claim(Authentication auth, String nom) {
        Map<String, Object> claims = Map.of();
        if (auth instanceof JwtAuthenticationToken jwt) {
            claims = jwt.getToken().getClaims();
        } else if (auth != null && auth.getPrincipal() instanceof OidcUser oidc) {
            claims = oidc.getClaims();
        }
        Object valeur = claims.get(nom);
        return valeur == null ? "" : valeur.toString();
    }
}
