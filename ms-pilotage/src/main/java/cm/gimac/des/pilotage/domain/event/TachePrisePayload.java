package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.task-taken.v1 (sujet pilotage.task, clé = tâche, D-93) : une tâche à prendre a trouvé son
 * responsable, par sa prise ({@code mode} PRISE, l'acteur est le responsable) ou par son attribution (ATTRIBUTION).
 * {@code origine} : celle donnée à la création ({@code origine.reference} permet au service qui l'a créée, par
 * exemple MS-HOMOLOGATION, de désigner la personne sur sa fonction) ; {@code profilsRequis} : profils de l'offre
 * (vide hors offre d'un autre service), {@code profilRequis} : profil d'une étape de processus,
 * {@code permissionsRequises} : permissions de l'offre (vide hors offre par permission, D-94).
 */
public record TachePrisePayload(
    UUID tacheId,
    Origine origine,
    String responsableId,
    String acteurId,
    String mode,
    List<String> profilsRequis,
    String profilRequis,
    String uniteCible,
    String titre,
    Instant le,
    List<String> permissionsRequises
) {
    public static final String TYPE = "pilotage.task-taken.v1";

    /** Origine de la tâche, telle que créée. */
    public record Origine(String type, String reference, String service, String lien) {}

    public TachePrisePayload {
        profilsRequis = profilsRequis == null ? List.of() : List.copyOf(profilsRequis);
        permissionsRequises = permissionsRequises == null ? List.of() : List.copyOf(permissionsRequises);
    }
}
