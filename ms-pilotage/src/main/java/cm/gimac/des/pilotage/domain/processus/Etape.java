package cm.gimac.des.pilotage.domain.processus;

import java.util.List;

/**
 * Étape d'un processus (nœud du graphe, §1) : action à effectuer, intervenant, validateur facultatif,
 * prédécesseurs (arcs), délai cible ({@code delaiJours}) et délai maximal ({@code delaiMaxJours}, D-106 : au-delà,
 * dépassement ; null = égal au délai cible), livrables, signatures et champs ; position sur le canevas.
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
    Position position,
    Integer delaiMaxJours
) {
    /** Étape antérieure à D-106 : délai maximal égal au délai cible. */
    public Etape(
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
        this(code, libelle, action, intervenant, validateur, predecesseurs, delaiJours, livrables, signatures, champs, position, null);
    }

    /** Délai maximal effectif (D-106) : le délai cible s'il n'est pas défini, jamais en deçà de la cible. */
    public int delaiMaxEffectif() {
        return delaiMaxJours == null ? delaiJours : Math.max(delaiMaxJours, delaiJours);
    }

    public Etape {
        predecesseurs = predecesseurs == null ? List.of() : List.copyOf(predecesseurs);
        livrables = livrables == null ? List.of() : List.copyOf(livrables);
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
        champs = champs == null ? List.of() : List.copyOf(champs);
        position = position == null ? new Position(0, 0) : position;
    }
}
