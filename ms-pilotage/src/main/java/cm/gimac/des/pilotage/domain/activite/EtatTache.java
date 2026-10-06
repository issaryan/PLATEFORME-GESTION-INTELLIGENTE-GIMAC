package cm.gimac.des.pilotage.domain.activite;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * État d'une tâche (§2.3 ; CC TaskState, p.238 et p.245). Transitions : A_PRENDRE (tâche confiée à une
 * unité, sans responsable, D-81 §6) vers OUVERTE (prise ou attribution) ou ANNULEE (créateur) ;
 * OUVERTE vers EN_COURS, BLOQUEE ou ANNULEE ; EN_COURS vers BLOQUEE, TERMINEE ou ANNULEE ; BLOQUEE
 * vers EN_COURS ou ANNULEE ; TERMINEE vers EN_COURS (réouverture). ANNULEE est final.
 */
public enum EtatTache {
    A_PRENDRE,
    OUVERTE,
    EN_COURS,
    BLOQUEE,
    TERMINEE,
    ANNULEE;

    private static final Map<EtatTache, Set<EtatTache>> SUIVANTS = Map.of(
        A_PRENDRE,
        EnumSet.of(OUVERTE, ANNULEE),
        OUVERTE,
        EnumSet.of(EN_COURS, BLOQUEE, ANNULEE),
        EN_COURS,
        EnumSet.of(BLOQUEE, TERMINEE, ANNULEE),
        BLOQUEE,
        EnumSet.of(EN_COURS, ANNULEE),
        TERMINEE,
        EnumSet.of(EN_COURS),
        ANNULEE,
        EnumSet.noneOf(EtatTache.class)
    );

    /** États d'une tâche encore à traiter (ni terminée, ni annulée), à prendre comprise. */
    public static final Set<EtatTache> OUVERTS = EnumSet.of(A_PRENDRE, OUVERTE, EN_COURS, BLOQUEE);

    public boolean peutPasserA(EtatTache cible) {
        return cible != null && SUIVANTS.get(this).contains(cible);
    }

    /** BLOQUEE et ANNULEE exigent un motif, comme la réouverture d'une tâche terminée. */
    public boolean motifExigePour(EtatTache cible) {
        return cible == BLOQUEE || cible == ANNULEE || (this == TERMINEE && cible == EN_COURS);
    }

    public boolean estOuvert() {
        return OUVERTS.contains(this);
    }
}
