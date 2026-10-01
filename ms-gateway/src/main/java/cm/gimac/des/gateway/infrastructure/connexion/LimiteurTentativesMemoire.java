package cm.gimac.des.gateway.infrastructure.connexion;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Limitation en mémoire de l'instance (seau à jetons par clé) : « capacite » tentatives, rechargées
 * au rythme de « capacite » par « fenetre ». Utilisée sans Valkey et quand Valkey ne répond pas.
 */
public class LimiteurTentativesMemoire implements LimiteurTentatives {

    private static final class Seau {

        double jetons;
        long dernier;

        Seau(double jetons, long dernier) {
            this.jetons = jetons;
            this.dernier = dernier;
        }
    }

    private final int capacite;
    private final Duration fenetre;
    private final Clock horloge;
    private final Cache<String, Seau> seaux;

    public LimiteurTentativesMemoire(int capacite, Duration fenetre, Clock horloge) {
        this.capacite = capacite;
        this.fenetre = fenetre;
        this.horloge = horloge;
        this.seaux = Caffeine.newBuilder().expireAfterAccess(fenetre.multipliedBy(2)).maximumSize(200_000).build();
    }

    @Override
    public Mono<Boolean> consommer(String cle) {
        return Mono.fromSupplier(() -> admettre(cle));
    }

    boolean admettre(String cle) {
        long maintenant = horloge.millis();
        Seau seau = seaux.get(cle, k -> new Seau(capacite, maintenant));
        synchronized (seau) {
            double recharge = (maintenant - seau.dernier) * (double) capacite / fenetre.toMillis();
            seau.jetons = Math.min(capacite, seau.jetons + Math.max(0, recharge));
            seau.dernier = maintenant;
            if (seau.jetons >= 1) {
                seau.jetons -= 1;
                return true;
            }
            return false;
        }
    }
}
