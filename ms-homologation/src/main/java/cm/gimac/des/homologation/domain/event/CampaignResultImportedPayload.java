package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * homologation.campaign-result-imported.v1 (sujet homologation.campaign, US-EXI-14) : résultats d'une campagne de la
 * sandbox repris par MS-HOMOLOGATION, en lecture seule. {@code enregistrementsLus} : enregistrements de la relève ;
 * {@code executionsRetenues} : exécutions de la campagne connues (plus haute position de chacune) ;
 * {@code scenariosNonRapproches} : exécutions dont le scénario est absent du référentiel de la liaison.
 */
public record CampaignResultImportedPayload(
    UUID liaisonId,
    String campagneId,
    String code,
    String etat,
    int enregistrementsLus,
    long executionsRetenues,
    long scenariosNonRapproches,
    int piecesConservees,
    Integer versionBilan,
    Instant releveLe
) {
    public static final String TYPE = "homologation.campaign-result-imported.v1";
}
