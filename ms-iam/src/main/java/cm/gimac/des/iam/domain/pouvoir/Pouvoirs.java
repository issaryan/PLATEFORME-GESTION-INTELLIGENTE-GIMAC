package cm.gimac.des.iam.domain.pouvoir;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pouvoirs d'un compte (§1, §3) : pour chaque action, la portée la plus large parmi ses profils ; profils
 * demandables : réunion de ceux de ses profils (jamais un profil privilégié).
 */
public record Pouvoirs(Map<ActionPouvoir, Portee> actions, Set<String> profilsDemandables) {
    public Pouvoirs {
        Map<ActionPouvoir, Portee> complete = new EnumMap<>(ActionPouvoir.class);
        for (ActionPouvoir a : ActionPouvoir.values()) {
            Portee p = actions == null ? null : actions.get(a);
            complete.put(a, p == null ? Portee.AUCUNE : p);
        }
        actions = Collections.unmodifiableMap(complete);
        profilsDemandables = Collections.unmodifiableSet(new TreeSet<>(profilsDemandables == null ? Set.of() : profilsDemandables));
    }

    public Portee portee(ActionPouvoir action) {
        return actions.get(action);
    }

    /** La plus large des portées de toutes les actions. */
    public Portee porteeMax() {
        return actions.values().stream().reduce(Portee.AUCUNE, Portee::max);
    }

    /**
     * Portée des comptes couverts (subordonnés de la vue « mes pouvoirs ») : la plus large, plafonnée à
     * UNITES, car TOUTES (D-92) ne vise que les processus d'unité, pas les personnes.
     */
    public Portee porteeSubordonnes() {
        return porteeMax().min(Portee.UNITES);
    }
}
