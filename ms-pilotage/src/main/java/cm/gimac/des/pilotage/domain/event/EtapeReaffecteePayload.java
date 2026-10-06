package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-step-reassigned.v1 (sujet pilotage.process, clé = instance, D-87) : étape
 * {@code etape} (libellé {@code libelleEtape}), ou l'une de ses signatures ou sa validation, réaffectée par le chef d'unité ({@code cause} REAFFECTATION) ou après le départ d'une
 * personne ({@code cause} DEPART : compte désactivé ou supprimé, remplacement par profil) ; {@code roles} :
 * rôles repris (EXECUTANT, VALIDATEUR, SIGNATURE.CODE) ; {@code destinataires} : nouvel exécutant ou porteurs du
 * profil de remplacement, et le chef de l'unité ({@code chefId}) pour un départ.
 */
public record EtapeReaffecteePayload(
    UUID instanceId,
    String code,
    String intitule,
    String etape,
    String libelleEtape,
    String cause,
    List<String> roles,
    String ancienId,
    String executantId,
    String profil,
    String unite,
    String chefId,
    String acteurId,
    String motif,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-step-reassigned.v1";

    public EtapeReaffecteePayload {
        roles = roles == null ? List.of() : List.copyOf(roles);
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
