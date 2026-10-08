package cm.gimac.des.homologation.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * homologation.function-created.v1 et homologation.function-updated.v1 (sujet homologation.function, D-91 §2) :
 * fonction du catalogue créée ou modifiée (libellé, profils, externe, active, permissions qui la caractérisent, D-94),
 * avec le motif d'une modification.
 * Journalisé par le registre d'audit.
 */
public record FunctionChangedPayload(
    String code,
    String libelle,
    List<String> profils,
    boolean externe,
    boolean actif,
    List<String> changements,
    String motif,
    UUID acteurId,
    Instant le,
    List<String> permissions
) {
    public static final String CREEE = "homologation.function-created.v1";
    public static final String MODIFIEE = "homologation.function-updated.v1";
}
