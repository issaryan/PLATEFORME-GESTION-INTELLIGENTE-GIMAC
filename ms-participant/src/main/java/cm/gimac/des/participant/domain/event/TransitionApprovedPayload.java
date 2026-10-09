package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/**
 * participant.transition-approved.v1 (D-98) : approbation enregistrée sur une proposition, publiée sur le sujet des
 * transitions (même clé que la proposition, donc dans l'ordre). {@code definitive} vrai : la transition est prononcée
 * (participant.status-changed.v1 part aussi) ; faux : première des deux approbations requises, la proposition attend une
 * seconde approbation d'une autre personne. {@code demandeurId}, {@code institutionNom} et {@code lien} (ajouts compatibles,
 * DDR-08) : MS-NOTIFICATION avise le proposant du prononcé.
 */
public record TransitionApprovedPayload(
    UUID institutionId,
    UUID transitionId,
    String de,
    String vers,
    UUID approbateurId,
    int approbationsRequises,
    int approbationsRecues,
    boolean definitive,
    UUID demandeurId,
    String institutionNom,
    String lien
) {
    public static final String TYPE = "participant.transition-approved.v1";
}
