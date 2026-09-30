package cm.gimac.des.certification.infrastructure.security;

import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Routes métier des utilisateurs (D-61) : le jeton doit provenir d'une authentification à deux facteurs,
 * attestée par le niveau d'assurance (claim acr) que Keycloak pose selon le flux suivi. Sur le portail,
 * l'institution du jeton est exigée en plus (RG-59). La matrice des rôles portée par ce même jeton décide
 * ensuite, route par route, par {@code @PreAuthorize}. Les jetons de service n'ont pas accès à ces routes.
 */
public class StrongAuthenticationAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    public static final String ACR_CLAIM = "acr";

    private final boolean exige;
    private final Set<String> niveauxAcceptes;
    private final boolean institutionExigee;

    public StrongAuthenticationAuthorizationManager(boolean exige, Set<String> niveauxAcceptes) {
        this(exige, niveauxAcceptes, false);
    }

    public StrongAuthenticationAuthorizationManager(boolean exige, Set<String> niveauxAcceptes, boolean institutionExigee) {
        this.exige = exige;
        this.niveauxAcceptes = Set.copyOf(niveauxAcceptes);
        this.institutionExigee = institutionExigee;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth instanceof JwtAuthenticationToken jwt)) {
            return new AuthorizationDecision(false);
        }
        if (institutionExigee) {
            String institution = jwt.getToken().getClaimAsString(CorrelationContext.INSTITUTION_CLAIM);
            if (institution == null || institution.isBlank()) {
                return new AuthorizationDecision(false);
            }
        }
        if (!exige) {
            return new AuthorizationDecision(true);
        }
        String acr = jwt.getToken().getClaimAsString(ACR_CLAIM);
        return new AuthorizationDecision(acr != null && niveauxAcceptes.contains(acr));
    }
}
