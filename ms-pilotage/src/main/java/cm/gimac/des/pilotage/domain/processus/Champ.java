package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/** Champ d'une étape (§1) : valeur saisie par l'exécutant ; {@code choix} : valeurs admises d'un champ CHOIX. */
public record Champ(String code, String libelle, TypeChamp type, List<String> choix, boolean obligatoire) {
    public Champ {
        choix = choix == null ? List.of() : List.copyOf(choix);
    }
}
