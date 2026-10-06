package cm.gimac.des.iam.domain.pouvoir;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ligne de la matrice des pouvoirs pour un profil (§3) : portée de chaque action (AUCUNE si elle n'est
 * pas renseignée) et profils qu'un titulaire peut demander pour un subordonné.
 */
public record PouvoirsProfil(String profil, Map<ActionPouvoir, Portee> actions, Set<String> profilsDemandables) {
    public PouvoirsProfil {
        Map<ActionPouvoir, Portee> complete = new EnumMap<>(ActionPouvoir.class);
        for (ActionPouvoir a : ActionPouvoir.values()) {
            Portee p = actions == null ? null : actions.get(a);
            complete.put(a, p == null ? Portee.AUCUNE : p);
        }
        actions = Collections.unmodifiableMap(complete);
        profilsDemandables = Collections.unmodifiableSet(new TreeSet<>(profilsDemandables == null ? Set.of() : profilsDemandables));
    }

    /** Ligne d'un profil absent de la matrice : AUCUNE partout, aucun profil demandable. */
    public static PouvoirsProfil aucun(String profil) {
        return new PouvoirsProfil(profil, Map.of(), Set.of());
    }

    public Portee portee(ActionPouvoir action) {
        return actions.get(action);
    }

    /** Représentation compacte pour le journal (avant, après). */
    public String resume() {
        StringBuilder sb = new StringBuilder();
        actions.forEach((a, p) -> {
            if (p != Portee.AUCUNE) {
                sb.append(a.name()).append('=').append(p.name()).append(' ');
            }
        });
        return (sb.isEmpty() ? "AUCUNE " : sb.toString()) + "demandables=" + String.join("|", profilsDemandables);
    }
}
