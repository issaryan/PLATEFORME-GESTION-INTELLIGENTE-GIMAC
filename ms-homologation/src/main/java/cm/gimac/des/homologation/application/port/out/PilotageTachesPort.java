package cm.gimac.des.homologation.application.port.out;

/**
 * Tâches de MS-PILOTAGE (D-76, docs/lot1/modelisateur-processus.md §5) : création idempotente par
 * (origine.reference, responsableId), clôture et réouverture, jeton de service homologation-service.
 */
public interface PilotageTachesPort {
    /** Issue d'un appel : fait, refusé définitivement (4xx), service indisponible (à réessayer). */
    enum Resultat {
        FAIT,
        REFUSE,
        INDISPONIBLE,
    }

    record Origine(String type, String reference, String service, String lien) {}

    record Tache(String titre, String description, String echeance, String priorite, String responsableId, String createurId, Origine origine) {}

    /** Désignation d'une tâche existante par son origine et son responsable (clôture, réouverture). */
    record Designation(Origine origine, String responsableId) {}

    Resultat creer(Tache t);

    Resultat cloturer(Designation d);

    Resultat rouvrir(Designation d);
}
