package cm.gimac.des.homologation.application.port.out;

import java.util.List;

/**
 * Tâches de MS-PILOTAGE (D-76, docs/lot1/modelisateur-processus.md §5) : création idempotente par
 * (origine.reference, responsableId), clôture et réouverture, jeton de service homologation-service. Tâche à prendre
 * (D-93, D-94, docs/lot1/effet-reel-referentiels.md §8.8) : sans responsable, avec {@code permissionsRequises}, idempotente
 * par origine.reference seule ; sa prise est publiée par pilotage.task-taken.v1.
 */
public interface PilotageTachesPort {
    /** Issue d'un appel : fait, refusé définitivement (4xx), service indisponible (à réessayer). */
    enum Resultat {
        FAIT,
        REFUSE,
        INDISPONIBLE,
    }

    record Origine(String type, String reference, String service, String lien) {}

    /** {@code permissionsRequises} : tâche à prendre par les porteurs de l'une des permissions (sans responsable). */
    record Tache(
        String titre,
        String description,
        String echeance,
        String priorite,
        String responsableId,
        String createurId,
        Origine origine,
        List<String> permissionsRequises
    ) {
        public Tache(String titre, String description, String echeance, String priorite, String responsableId, String createurId, Origine origine) {
            this(titre, description, echeance, priorite, responsableId, createurId, origine, null);
        }
    }

    /**
     * Désignation d'une tâche existante par son origine et son responsable (clôture, réouverture) ; {@code aPrendre} vrai :
     * la tâche à prendre de l'origine seule, remise à prendre (sans responsable) à sa réouverture.
     */
    record Designation(Origine origine, String responsableId, Boolean aPrendre) {
        public Designation(Origine origine, String responsableId) {
            this(origine, responsableId, null);
        }

        /** Tâche à prendre de l'origine. */
        public static Designation offre(Origine origine) {
            return new Designation(origine, null, Boolean.TRUE);
        }
    }

    Resultat creer(Tache t);

    Resultat cloturer(Designation d);

    Resultat rouvrir(Designation d);
}
