package cm.gimac.des.keycloak.totp;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.resource.RealmResourceProvider;

/** Fournit la ressource JAX-RS de l'extension, authentifiée par le jeton porteur de la requête. */
public class GimacTotpResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public GimacTotpResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new GimacTotpResource(session, s -> new AppAuthManager.BearerTokenAuthenticator(s).authenticate());
    }

    @Override
    public void close() {
        // Rien à fermer : la session appartient à Keycloak.
    }
}
