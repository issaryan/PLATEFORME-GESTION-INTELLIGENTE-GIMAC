package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Public d'un processus (D-98 §1, D-100) : qui peut le voir dans « Lancer une demande » et le lancer, règles cumulables
 * et leurs paramètres (unités, profils, permissions, personnes désignées nommément). Absents ou vides : les membres de
 * l'unité propriétaire et ses responsables ({@link RegleInitiateur#MEMBRES_UNITE}). Les définitions antérieures à D-100
 * n'ont pas de {@code personnes} : liste vide. {@code initiateurIntervenant} (D-99, séparation des fonctions) : vrai si
 * l'initiateur d'une instance peut aussi exécuter, valider ou signer les étapes de sa propre demande ; faux par défaut
 * (et pour les définitions antérieures).
 */
public record Initiateurs(
    List<RegleInitiateur> regles,
    List<String> unites,
    List<String> profils,
    List<String> permissions,
    List<String> personnes,
    boolean initiateurIntervenant
) {
    public Initiateurs {
        regles = regles == null || regles.isEmpty() ? List.of(RegleInitiateur.MEMBRES_UNITE) : List.copyOf(regles);
        unites = unites == null ? List.of() : List.copyOf(unites);
        profils = profils == null ? List.of() : List.copyOf(profils);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        personnes = personnes == null ? List.of() : List.copyOf(personnes);
    }

    /** Règle par défaut : les membres de l'unité propriétaire et ses responsables. */
    public static Initiateurs parDefaut() {
        return new Initiateurs(List.of(RegleInitiateur.MEMBRES_UNITE), List.of(), List.of(), List.of(), List.of(), false);
    }

    public boolean a(RegleInitiateur r) {
        return regles.contains(r);
    }
}
