package cm.gimac.des.monitoring.application.port.out;

import cm.gimac.des.monitoring.domain.exploitation.ModeleLineaire;
import java.time.Duration;

/**
 * Port du solveur de programmation linéaire en nombres entiers (D-15, conservé) : résout le modèle construit par le
 * domaine dans la limite de temps donnée (300 s par défaut). Adaptateur : Google OR-Tools, moteur CP-SAT.
 */
public interface IntegerProgramSolver {
    Resultat resoudre(ModeleLineaire modele, Duration limite);

    /** Statut de la résolution. */
    enum Statut {
        OPTIMALE,
        REALISABLE,
        IRREALISABLE,
        SANS_SOLUTION_DANS_LE_DELAI,
    }

    /**
     * Résultat : valeurs des variables (vides sans solution), valeur de l'objectif, meilleure borne inférieure connue
     * et durée de la résolution.
     */
    record Resultat(Statut statut, long[] valeurs, long objectif, long borne, long dureeMs) {
        public boolean avecSolution() {
            return statut == Statut.OPTIMALE || statut == Statut.REALISABLE;
        }

        /** Écart d'optimalité relatif, 0 pour une solution optimale. */
        public double ecart() {
            if (!avecSolution() || statut == Statut.OPTIMALE) {
                return 0;
            }
            return Math.abs(objectif - borne) / (double) Math.max(1, Math.abs(objectif));
        }
    }
}
