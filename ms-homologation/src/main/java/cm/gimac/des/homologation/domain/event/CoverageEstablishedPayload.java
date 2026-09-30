package cm.gimac.des.homologation.domain.event;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * homologation.coverage-established.v1 (sujet homologation.liaison) : couverture de certification
 * constatée (rang 17). Lu par MS-CERTIFICATION : procès-verbal établissable à 100 % (docs/lot1/parcours-backend.md §3).
 */
public record CoverageEstablishedPayload(
    UUID liaisonId,
    UUID institutionId,
    Integer taux,
    int numerateur,
    int denominateur,
    List<String> servicesCouverts,
    LocalDate constateLe,
    UUID constatePar
) {
    public static final String TYPE = "homologation.coverage-established.v1";
}
