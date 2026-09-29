package cm.gimac.des.homologation.domain.event;

import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import java.time.Instant;
import java.util.UUID;

/**
 * Charge utile de homologation.catalog-published.v1 (HOM-01). Le plan la nomme
 * homologation.referential-published.v1 ; D-37 retient le nom du cahier de conception.
 */
public record CatalogPublishedPayload(
    UUID versionId,
    TypeProjet typeProjet,
    int numero,
    Instant datePublication,
    int nombreOperations,
    UUID versionSourceId
) {
    public static final String TYPE = "homologation.catalog-published.v1";
}
