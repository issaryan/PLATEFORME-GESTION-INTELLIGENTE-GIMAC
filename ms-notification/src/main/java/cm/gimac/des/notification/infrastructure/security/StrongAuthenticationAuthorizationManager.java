package cm.gimac.des.notification.infrastructure.security;

import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Routes métier des utilisateurs (D-61) : le jeton doit provenir d'une authentification à deux
 * facteurs, attestée par le niveau d'assurance (claim acr) que Keycloak pose selon le flux suivi.
 * La matrice des rôles portée par ce même jeton décide ensuite, route par route, par
 * {@code @PreAuthorize}. Les jetons de service (client_credentials) n'ont pas accès à ces routes.
 */
public class StrongAuthenticationAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    public static final String ACR_CLAIM = "acr";

    private final boolean exige;
    private final Set<String> niveauxAcceptes;

    public StrongAuthenticationAuthorizationManager(boolean exige, Set<String> niveauxAcceptes) {
        this.exige = exige;
        this.niveauxAcceptes = Set.copyOf(niveauxAcceptes);
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth instanceof JwtAuthenticationToken jwt)) {
            return new AuthorizationDecision(false);
        }
        if (!exige) {
            return new AuthorizationDecision(true);
        }
        String acr = jwt.getToken().getClaimAsString(ACR_CLAIM);
        return new AuthorizationDecision(acr != null && niveauxAcceptes.contains(acr));
    }
}
