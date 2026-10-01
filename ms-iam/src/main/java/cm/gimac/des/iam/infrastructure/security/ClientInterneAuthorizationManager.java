package cm.gimac.des.iam.infrastructure.security;

import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Routes internes de la connexion (D-65) : jeton de service portant la permission exigée, émis pour
 * l'un des clients admis (claim {@code azp} ou {@code client_id}, la passerelle {@code gateway-service}).
 * Un autre service, même porteur de INTERNAL_IAM_READ, n'y a pas accès.
 */
public class ClientInterneAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final String permission;
    private final Set<String> clients;

    public ClientInterneAuthorizationManager(String permission, Set<String> clients) {
        this.permission = permission;
        this.clients = Set.copyOf(clients);
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth instanceof JwtAuthenticationToken jwt)) {
            return new AuthorizationDecision(false);
        }
        boolean permis = auth.getAuthorities().stream().anyMatch(a -> permission.equals(a.getAuthority()));
        String client = jwt.getToken().getClaimAsString("azp");
        if (client == null) {
            client = jwt.getToken().getClaimAsString("client_id");
        }
        return new AuthorizationDecision(permis && (clients.isEmpty() || client != null && clients.contains(client)));
    }
}
