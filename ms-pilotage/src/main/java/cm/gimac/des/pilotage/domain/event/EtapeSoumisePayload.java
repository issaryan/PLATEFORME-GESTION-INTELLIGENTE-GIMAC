package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-step-submitted.v1 (sujet pilotage.process, clé = instance, D-87) : fin déclarée par
 * l'exécutant, l'étape attend ses signatures ({@code attente} SIGNATURE, {@code signatures} : codes ouverts) ou sa
 * validation (VALIDATION) ; {@code destinataires} : signataires attendus ou validateur (personnes nommées, ou
 * porteurs du profil dans l'unité).
 */
public record EtapeSoumisePayload(
    UUID instanceId,
    String code,
    String intitule,
    String etape,
    String libelleEtape,
    String attente,
    List<String> signatures,
    String acteurId,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-step-submitted.v1";

    public EtapeSoumisePayload {
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
