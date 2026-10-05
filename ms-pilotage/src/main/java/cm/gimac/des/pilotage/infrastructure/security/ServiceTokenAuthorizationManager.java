package cm.gimac.des.pilotage.infrastructure.security;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Interfaces internes (/api/v1/interne/**, PI §0.7) : accès réservé à un jeton de service
 * (client_credentials) qui porte la permission interne de la route. Un jeton utilisateur est
 * refusé même s'il porte cette permission.
 *
 * Un jeton de service Keycloak se reconnaît à son compte de service : {@code preferred_username}
 * vaut {@code service-account-<client>}, où {@code <client>} est le client qui a obtenu le jeton
 * ({@code azp}, à défaut {@code client_id}). Un jeton utilisateur a pour {@code azp} le client
 * de l'interface et pour {@code preferred_username} l'identifiant de la personne.
 */
public class ServiceTokenAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    public static final String PREFIXE_COMPTE_DE_SERVICE = "service-account-";
    static final String CLAIM_NOM = "preferred_username";
    static final String CLAIM_CLIENT = "azp";
    static final String CLAIM_CLIENT_ID = "client_id";

    private final String permission;

    public ServiceTokenAuthorizationManager(String permission) {
        this.permission = permission;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || !(auth instanceof JwtAuthenticationToken jwt) || clientDeService(jwt.getToken()).isEmpty()) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(permission::equals));
    }

    /** Client appelant si le jeton est un jeton de service, vide pour un jeton utilisateur. */
    public static Optional<String> clientDeService(Jwt jeton) {
        String client = jeton.getClaimAsString(CLAIM_CLIENT);
        if (client == null || client.isBlank()) {
            client = jeton.getClaimAsString(CLAIM_CLIENT_ID);
        }
        String nom = jeton.getClaimAsString(CLAIM_NOM);
        if (client == null || client.isBlank() || nom == null || !nom.equalsIgnoreCase(PREFIXE_COMPTE_DE_SERVICE + client)) {
            return Optional.empty();
        }
        return Optional.of(client);
    }
}
