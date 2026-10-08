package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Initiateurs d'un processus (D-98 §1) : règles cumulables et leurs paramètres (unités, profils, permissions).
 * Absents ou vides : les membres de l'unité propriétaire ({@link RegleInitiateur#MEMBRES_UNITE}).
 */
public record Initiateurs(List<RegleInitiateur> regles, List<String> unites, List<String> profils, List<String> permissions) {
    public Initiateurs {
        regles = regles == null || regles.isEmpty() ? List.of(RegleInitiateur.MEMBRES_UNITE) : List.copyOf(regles);
        unites = unites == null ? List.of() : List.copyOf(unites);
        profils = profils == null ? List.of() : List.copyOf(profils);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    /** Règle par défaut : les membres de l'unité propriétaire. */
    public static Initiateurs parDefaut() {
        return new Initiateurs(List.of(RegleInitiateur.MEMBRES_UNITE), List.of(), List.of(), List.of());
    }

    public boolean a(RegleInitiateur r) {
        return regles.contains(r);
    }
}
