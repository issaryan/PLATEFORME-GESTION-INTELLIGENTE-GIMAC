package cm.gimac.des.gateway.security.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.server.WebSession;

/**
 * Sessions ouvertes par la connexion, parcourues par le balayage pour fermer celles qui ont
 * atteint leur échéance même sans nouvelle requête ; identifiants des sessions fermées pour
 * échéance, retenus pour répondre CNX_SESSION_EXPIREE au témoin qui les désigne encore.
 */
public class RegistreSessions {

    private final Set<WebSession> sessions = ConcurrentHashMap.newKeySet();
    private final Cache<String, Boolean> expirees;

    public RegistreSessions(Duration retention) {
        this.expirees = Caffeine.newBuilder().expireAfterWrite(retention).maximumSize(100_000).build();
    }

    public void enregistrer(WebSession session) {
        sessions.add(session);
    }

    public void retirer(WebSession session) {
        sessions.remove(session);
    }

    public List<WebSession> sessions() {
        return List.copyOf(sessions);
    }

    public void marquerExpiree(String identifiant) {
        expirees.put(identifiant, Boolean.TRUE);
    }

    public boolean estExpiree(String identifiant) {
        return identifiant != null && expirees.getIfPresent(identifiant) != null;
    }

    /** Une nouvelle connexion réutilise le témoin : l'ancien identifiant n'est plus signalé. */
    public void oublierExpiree(String identifiant) {
        if (identifiant != null) {
            expirees.invalidate(identifiant);
        }
    }
}
