package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/** Formulaire de lancement d'un processus (D-98 §2) : champs typés remplis par l'initiateur, en plus de l'objet. */
public record Formulaire(List<ChampFormulaire> champs) {
    public Formulaire {
        champs = champs == null ? List.of() : List.copyOf(champs);
    }

    public static Formulaire vide() {
        return new Formulaire(List.of());
    }
}
