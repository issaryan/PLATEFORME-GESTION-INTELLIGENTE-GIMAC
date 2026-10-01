package cm.gimac.des.keycloak.totp;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Fabrique de l'extension « gimac-totp » (D-65) : ressources servies sous
 * {@code /realms/{realm}/gimac-totp}.
 */
public class GimacTotpResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "gimac-totp";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new GimacTotpResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // Aucun paramètre.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Rien à préparer.
    }

    @Override
    public void close() {
        // Aucune ressource tenue.
    }

    @Override
    public String getId() {
        return ID;
    }
}
