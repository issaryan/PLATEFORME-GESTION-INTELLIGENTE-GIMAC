package cm.gimac.des.participant.domain.event;

import java.util.UUID;

/**
 * participant.transition-rejected.v1 : proposition rejetée par l'approbateur, avec motif (PAR-02, critère 2).
 *
 * <p>{@code demandeurId}, {@code institutionNom} et {@code lien} (ajouts compatibles, DDR-08) : MS-NOTIFICATION avise le
 * proposant du refus, avec le lien de l'onglet des propositions de statut.
 */
public record TransitionRejectedPayload(
    UUID institutionId,
    UUID transitionId,
    String de,
    String vers,
    String motif,
    UUID approbateurId,
    UUID demandeurId,
    String institutionNom,
    String lien
) {
    public static final String TYPE = "participant.transition-rejected.v1";
}
