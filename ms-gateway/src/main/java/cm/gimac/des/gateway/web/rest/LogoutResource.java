package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.security.session.FermetureSession;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

/**
 * Déconnexion (D-65) : la session Keycloak est fermée de serveur à serveur (révocation du jeton de
 * rafraîchissement), puis la session de la passerelle. Aucune adresse du fournisseur d'identité
 * n'est renvoyée au navigateur : l'utilisateur revient à l'écran de connexion de sa couche.
 */
@RestController
public class LogoutResource {

    private final ObjectProvider<FermetureSession> fermeture;

    public LogoutResource(ObjectProvider<FermetureSession> fermeture) {
        this.fermeture = fermeture;
    }

    /**
     * {@code POST /api/logout} : ferme la session de l'utilisateur.
     *
     * @param session la session courante
     * @return {@code 200 (OK)} et un corps vide ({@code {}}), sans « logoutUrl »
     */
    @PostMapping("/api/logout")
    public Mono<Map<String, String>> logout(WebSession session) {
        FermetureSession f = fermeture.getIfAvailable();
        Mono<Void> fermer = f == null ? session.invalidate() : f.fermer(session, false);
        return fermer.thenReturn(Map.of());
    }
}
