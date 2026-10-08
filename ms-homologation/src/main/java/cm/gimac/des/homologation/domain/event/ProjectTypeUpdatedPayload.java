package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * homologation.project-type-updated.v1 (sujet homologation.project, D-91 §1) : libellé, description, modèle (avant
 * la première publication) ou état actif d'un type modifiés, avec le motif. Journalisé par le registre d'audit.
 */
public record ProjectTypeUpdatedPayload(
    String code,
    String libelle,
    String description,
    String modele,
    boolean actif,
    List<String> changements,
    String motif,
    UUID acteurId,
    Instant le
) {
    public static final String TYPE = "homologation.project-type-updated.v1";
}
