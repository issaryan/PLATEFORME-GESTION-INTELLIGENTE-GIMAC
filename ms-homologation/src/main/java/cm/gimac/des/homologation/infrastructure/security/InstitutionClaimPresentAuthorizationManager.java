package cm.gimac.des.homologation.infrastructure.security;

import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Routes du portail des participants (PI-GIMAC-01 §0.6) : accès refusé si le jeton ne porte
 * pas d'institution. Le cloisonnement des données par institution (RG-59) s'appuie ensuite
 * sur ce même claim.
 */
public class InstitutionClaimPresentAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth instanceof JwtAuthenticationToken jwt && auth.isAuthenticated()) {
            String institution = jwt.getToken().getClaimAsString(CorrelationContext.INSTITUTION_CLAIM);
            return new AuthorizationDecision(institution != null && !institution.isBlank());
        }
        return new AuthorizationDecision(false);
    }
}
