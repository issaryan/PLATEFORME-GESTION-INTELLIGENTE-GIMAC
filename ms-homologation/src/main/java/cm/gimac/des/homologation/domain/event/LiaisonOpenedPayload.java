package cm.gimac.des.homologation.domain.event;

import java.util.List;
import java.util.UUID;

/**
 * homologation.liaison-opened.v1 (sujet homologation.liaison) : liaison ouverte au cadrage. Lu par
 * MS-PARTICIPANT, qui fait passer l'adhérent à EN_HOMOLOGATION (D-57), et par MS-AUDIT.
 */
public record LiaisonOpenedPayload(
    UUID liaisonId,
    UUID institutionId,
    String typeProjet,
    UUID versionId,
    Integer versionNumero,
    UUID chefDeProjetId,
    UUID banquePartenaireId,
    List<String> perimetre,
    UUID ouvertPar
) {
    public static final String TYPE = "homologation.liaison-opened.v1";
}
