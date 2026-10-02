package cm.gimac.des.gateway.domain.live;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Présence des comptes internes (D-72) : un compte est en ligne tant qu'au moins une connexion au
 * canal poussé d'une couche interne lui appartient (plusieurs onglets ou appareils). Dernière
 * présence retenue à la fermeture de sa dernière connexion. En mémoire de l'instance de passerelle.
 * Sûr entre fils d'exécution.
 */
public class RegistrePresence {

    /** Comptes en ligne et dernière présence des autres. */
    public record Etat(Set<String> enLigne, Map<String, Instant> vusLe) {}

    private final Clock horloge;
    private final Map<String, Integer> connexions = new HashMap<>();
    private final Map<String, Instant> vusLe = new HashMap<>();

    public RegistrePresence(Clock horloge) {
        this.horloge = horloge;
    }

    /** Nouvelle connexion du compte ; vrai s'il n'en avait aucune (il passe en ligne). */
    public synchronized boolean connecter(String compte) {
        return connexions.merge(compte, 1, Integer::sum) == 1;
    }

    /** Fin d'une connexion du compte ; vrai si c'était la dernière (il quitte la ligne). */
    public synchronized boolean deconnecter(String compte) {
        Integer n = connexions.get(compte);
        if (n == null) {
            return false;
        }
        if (n > 1) {
            connexions.put(compte, n - 1);
            return false;
        }
        connexions.remove(compte);
        vusLe.put(compte, horloge.instant());
        return true;
    }

    public synchronized boolean enLigne(String compte) {
        return connexions.containsKey(compte);
    }

    public synchronized Etat etat() {
        Map<String, Instant> vus = new HashMap<>(vusLe);
        vus.keySet().removeAll(connexions.keySet());
        return new Etat(new TreeSet<>(connexions.keySet()), Map.copyOf(vus));
    }
}
