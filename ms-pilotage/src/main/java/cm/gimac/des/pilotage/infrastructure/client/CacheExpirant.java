package cm.gimac.des.pilotage.infrastructure.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Cache en mémoire à durée de vie fixe (docs/lot1/mon-activite.md §3 : équipe mise en cache 60 s). Une
 * valeur expirée est relue à la demande suivante ; un échec de lecture n'est pas mis en cache.
 */
final class CacheExpirant<K, V> {

    private record Entree<V>(V valeur, long expireA) {}

    /** Borne du nombre d'entrées : au-delà, le cache est vidé (les comptes internes se comptent en centaines). */
    static final int TAILLE_MAX = 10_000;

    private final Map<K, Entree<V>> entrees = new ConcurrentHashMap<>();
    private final long dureeNanos;
    private final LongSupplier horloge;

    CacheExpirant(Duration duree) {
        this(duree, System::nanoTime);
    }

    CacheExpirant(Duration duree, LongSupplier horlogeNanos) {
        this.dureeNanos = duree.toNanos();
        this.horloge = horlogeNanos;
    }

    V lire(K cle, Function<K, V> chargeur) {
        long maintenant = horloge.getAsLong();
        Entree<V> e = entrees.get(cle);
        if (e != null && maintenant - e.expireA() < 0) {
            return e.valeur();
        }
        V v = chargeur.apply(cle);
        if (entrees.size() >= TAILLE_MAX) {
            entrees.clear();
        }
        entrees.put(cle, new Entree<>(v, maintenant + dureeNanos));
        return v;
    }

    void vider() {
        entrees.clear();
    }
}
