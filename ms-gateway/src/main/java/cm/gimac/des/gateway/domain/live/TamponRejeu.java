package cm.gimac.des.gateway.domain.live;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Tampon de reprise en mémoire : derniers événements reçus par l'instance, bornés en nombre et
 * en âge, pour honorer « depuis » (ou Last-Event-ID) à la reconnexion d'un navigateur.
 * Sûr entre fils d'exécution.
 */
public class TamponRejeu {

    private record Entree(EvenementLive evenement, Instant recuLe) {}

    private final int capacite;
    private final Duration duree;
    private final Clock horloge;
    private final Deque<Entree> entrees = new ArrayDeque<>();

    public TamponRejeu(int capacite, Duration duree, Clock horloge) {
        if (capacite < 1) {
            throw new IllegalArgumentException("capacité du tampon de reprise : au moins 1");
        }
        this.capacite = capacite;
        this.duree = duree;
        this.horloge = horloge;
    }

    public synchronized void ajouter(EvenementLive e) {
        entrees.addLast(new Entree(e, horloge.instant()));
        while (entrees.size() > capacite) {
            entrees.removeFirst();
        }
    }

    /**
     * Événements reçus après celui d'identifiant {@code dernier}. Sans identifiant : aucun (première
     * connexion). Identifiant inconnu ou expiré : tout le tampon, car des événements ont pu être
     * manqués ; le frontend n'en fait qu'invalider des requêtes, un doublon est sans effet.
     */
    public synchronized List<EvenementLive> depuis(String dernier) {
        purger();
        if (dernier == null || dernier.isBlank()) {
            return List.of();
        }
        List<EvenementLive> apres = new ArrayList<>();
        boolean trouve = false;
        for (Entree entree : entrees) {
            if (trouve) {
                apres.add(entree.evenement());
            } else if (dernier.equals(entree.evenement().id())) {
                trouve = true;
            }
        }
        return trouve ? apres : entrees.stream().map(Entree::evenement).toList();
    }

    public synchronized int taille() {
        purger();
        return entrees.size();
    }

    private void purger() {
        Instant limite = horloge.instant().minus(duree);
        while (!entrees.isEmpty() && entrees.peekFirst().recuLe().isBefore(limite)) {
            entrees.removeFirst();
        }
    }
}
