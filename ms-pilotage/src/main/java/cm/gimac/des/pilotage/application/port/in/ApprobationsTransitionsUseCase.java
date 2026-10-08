package cm.gimac.des.pilotage.application.port.in;

/**
 * Tâche de l'approbateur d'une transition de statut d'une institution (D-98, docs/lot1/lancement-processus.md §8.8) :
 * MS-PARTICIPANT publie la proposition (participant.transition-requested.v1), l'approbation
 * (participant.transition-approved.v1) et le refus (participant.transition-rejected.v1) ; MS-PILOTAGE en tient une tâche
 * à prendre par les porteurs de PAR_TRANSITION_APPROVE, close quand la transition est prononcée ou refusée.
 */
public interface ApprobationsTransitionsUseCase {
    /** Proposition en attente d'approbation. */
    record Proposition(
        String institutionId,
        String transitionId,
        String de,
        String vers,
        String motif,
        String demandeurId,
        int approbationsRequises,
        String institutionNom,
        String lien
    ) {}

    /** Tâche à prendre créée (idempotent : une seule tâche par proposition). Vrai si elle vient d'être créée. */
    boolean proposee(Proposition p);

    /**
     * Approbation enregistrée : {@code definitive} vrai, la tâche est terminée ; sinon (première de deux approbations), la
     * tâche est remise à prendre pour un second approbateur. Rend le nombre de tâches visées.
     */
    int approuvee(String institutionId, String transitionId, boolean definitive);

    /** Proposition refusée : la tâche est terminée. Rend le nombre de tâches visées. */
    int rejetee(String institutionId, String transitionId);
}
