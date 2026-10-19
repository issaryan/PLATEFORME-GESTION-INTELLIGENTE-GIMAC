package cm.gimac.des.monitoring.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Congés et absences validés dans MS-IAM (BF-ADMIN, UC-SOC-06) sur une période : lus à la génération de la grille
 * (GET /api/v1/interne/disponibilites de MS-IAM, jeton de service). MS-IAM injoignable : 503
 * MON_DISPONIBILITES_INDISPONIBLES.
 */
public interface DisponibilitesPort {
    List<Absence> absencesValidees(Instant du, Instant au);

    /** Absence validée d'un compte, annulée ou non. */
    record Absence(UUID id, String compteId, Instant debut, Instant fin, boolean annulee) {}
}
