package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Étape d'un processus (nœud du graphe, §1) : action à effectuer, intervenant, validateur facultatif,
 * prédécesseurs (arcs), délai, livrables, signatures et champs ; position sur le canevas.
 */
public record Etape(
    String code,
    String libelle,
    String action,
    Intervenant intervenant,
    Intervenant validateur,
    List<String> predecesseurs,
    int delaiJours,
    List<Livrable> livrables,
    List<SignatureRequise> signatures,
    List<Champ> champs,
    Position position
) {
    public Etape {
        predecesseurs = predecesseurs == null ? List.of() : List.copyOf(predecesseurs);
        livrables = livrables == null ? List.of() : List.copyOf(livrables);
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
        champs = champs == null ? List.of() : List.copyOf(champs);
        position = position == null ? new Position(0, 0) : position;
    }
}
