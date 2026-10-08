package cm.gimac.des.homologation.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * homologation.liaison-milestone-reached.v1 (sujet homologation.liaison, clé = liaison, D-93 §1 et §4) : l'opération
 * qui porte un jalon du référentiel est faite. MS-PARTICIPANT fait passer l'institution à ACTIF au jalon EN_PRODUCTION ;
 * MS-NOTIFICATION notifie le conducteur, les C et I de l'opération et l'institution ; l'audit le journalise.
 * {@code statut} : statut du CR qui en résulte ; {@code etatLiaison} : automate technique projeté (D-57).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiaisonMilestoneReachedPayload(
    UUID liaisonId,
    String reference,
    UUID institutionId,
    String institutionNom,
    String typeProjet,
    String modeleTypeProjet,
    String jalon,
    String libelleJalon,
    String statut,
    String etatLiaison,
    UUID operationId,
    String code,
    String libelle,
    UUID versionId,
    Integer versionReferentiel,
    String conducteurId,
    List<DestinataireOperation> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "homologation.liaison-milestone-reached.v1";
}
