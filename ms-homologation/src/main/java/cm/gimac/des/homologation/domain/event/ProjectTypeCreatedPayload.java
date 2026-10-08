package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * homologation.project-type-created.v1 (sujet homologation.project, D-91 §1) : type de projet créé avec le brouillon
 * v1 de son référentiel, vierge ou copié de la version active d'un autre type. Journalisé par le registre d'audit.
 */
public record ProjectTypeCreatedPayload(
    String code,
    String libelle,
    String description,
    String modele,
    boolean actif,
    String mode,
    String typeSource,
    UUID versionSourceId,
    UUID brouillonId,
    UUID acteurId,
    Instant le
) {
    public static final String TYPE = "homologation.project-type-created.v1";
}
