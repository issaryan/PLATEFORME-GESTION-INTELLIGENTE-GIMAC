package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/**
 * participant.transition-requested.v1 : proposition de transition en attente d'approbation (6.25, étape 2).
 *
 * <p>{@code institutionNom} et {@code lien} (ajouts compatibles du 08/10/2026, D-98) : nom de l'institution et écran de
 * l'application interne où l'approbateur prononce ou refuse la transition ; MS-PILOTAGE en fait une tâche à prendre par
 * les porteurs de PAR_TRANSITION_APPROVE (docs/lot1/lancement-processus.md §8.8).
 */
public record TransitionRequestedPayload(
    UUID institutionId,
    UUID transitionId,
    String de,
    String vers,
    String motif,
    UUID demandeurId,
    int approbationsRequises,
    String institutionNom,
    String lien
) {
    public static final String TYPE = "participant.transition-requested.v1";

    /** Écran où l'on prononce une transition : onglet « Transitions » de la fiche de l'institution. */
    public static String lienTransitions(UUID institutionId) {
        return "/des/institutions/" + institutionId + "?onglet=transitions";
    }
}
