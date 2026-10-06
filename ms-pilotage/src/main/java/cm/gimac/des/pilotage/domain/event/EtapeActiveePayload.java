package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-step-activated.v1 (sujet pilotage.process, clé = instance, D-87) : étape activée
 * (ou rendue active par un renvoi du validateur, {@code renvoi} vrai, {@code motif}) ; {@code executantId} :
 * personne chargée, ou null pour une tâche à prendre par les porteurs de {@code profil} dans {@code unite} ;
 * {@code destinataires} : l'exécutant, ou les membres actifs de l'unité portant le profil (à défaut le chef de
 * l'unité), que MS-NOTIFICATION avertit (type PROCESSUS).
 */
public record EtapeActiveePayload(
    UUID instanceId,
    UUID processusId,
    String code,
    String intitule,
    String etape,
    String libelleEtape,
    UUID tacheId,
    String executantId,
    String profil,
    String unite,
    String echeance,
    boolean renvoi,
    String motif,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-step-activated.v1";

    public EtapeActiveePayload {
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
