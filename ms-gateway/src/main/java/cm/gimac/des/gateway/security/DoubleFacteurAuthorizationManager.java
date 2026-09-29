package cm.gimac.des.gateway.security;

import java.util.Map;
import java.util.Set;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

/**
 * Accès réservé aux sessions ouvertes par une authentification à deux facteurs (D-61) :
 * utilisateur authentifié dont le jeton porte un niveau d'assurance (acr) accepté, 2 ou 3 par
 * défaut. Le claim est lu dans le jeton d'identité de la session OIDC de la passerelle, ou dans
 * le jeton porteur. Sans authentification, la décision négative mène au 401 ; authentifié sans
 * second facteur, au 403.
 */
public class DoubleFacteurAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    public static final String ACR = "acr";

    private final Set<String> niveauxAcceptes;

    public DoubleFacteurAuthorizationManager(Set<String> niveauxAcceptes) {
        this.niveauxAcceptes = Set.copyOf(niveauxAcceptes);
    }

    @Override
    public Mono<AuthorizationResult> authorize(Mono<Authentication> authentification, AuthorizationContext contexte) {
        return authentification
            .filter(Authentication::isAuthenticated)
            .<AuthorizationResult>map(auth -> new AuthorizationDecision(niveauxAcceptes.contains(acr(auth))))
            .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /** Niveau d'assurance du jeton, chaîne vide s'il est absent. */
    public static String acr(Authentication auth) {
        Map<String, Object> claims = Map.of();
        if (auth instanceof JwtAuthenticationToken jwt) {
            claims = jwt.getToken().getClaims();
        } else if (auth.getPrincipal() instanceof OidcUser oidc) {
            claims = oidc.getClaims();
        }
        Object valeur = claims.get(ACR);
        return valeur == null ? "" : valeur.toString();
    }
}
