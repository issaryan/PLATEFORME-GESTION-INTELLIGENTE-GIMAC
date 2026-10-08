package cm.gimac.des.pilotage.application.port.in;

import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.activite.TacheHistorique;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tâches (docs/lot1/mon-activite.md §2.3 ; CP PIL-04, PIL-08) et tâches confiées
 * (docs/lot1/pouvoirs-hierarchiques.md §6, D-81) : à une personne, à plusieurs (groupe), à une unité
 * (à prendre), relance.
 */
public interface TachesUseCase {
    /**
     * Saisie d'une tâche : {@code responsableId} (une personne, soi par défaut), {@code responsables} (2 à 50
     * personnes, un groupe) ou {@code uniteCible} (une unité, tâche à prendre), exclusifs.
     */
    record TacheSaisie(String titre, String description, String echeance, String priorite, String responsableId, List<String> responsables, String uniteCible) {
        public TacheSaisie(String titre, String description, String echeance, String priorite, String responsableId) {
            this(titre, description, echeance, priorite, responsableId, null, null);
        }
    }

    /** Avancement d'un groupe : tâches non annulées, terminées, en retard. */
    record Avancement(int total, int terminees, int enRetard) {}

    /** Tâches d'un envoi à plusieurs personnes, visibles de l'appelant. */
    record GroupeLu(UUID groupeId, String titre, String createurId, List<TacheLue> taches, Map<String, String> noms, Avancement avancement) {}

    record Origine(String type, String reference, String service, String lien) {}

    /**
     * Tâche demandée par un autre service (jeton de service INTERNAL_PILOTAGE_TASK_CREATE) : à {@code responsableId},
     * ou, sans responsable, à prendre par les porteurs de l'un des {@code profilsRequis} (1 à 20), dans
     * {@code uniteCible} si elle est donnée, sinon dans toute l'organisation (D-93).
     */
    record TacheInterne(
        String titre,
        String description,
        String echeance,
        String priorite,
        String responsableId,
        String createurId,
        Origine origine,
        List<String> profilsRequis,
        String uniteCible,
        List<String> permissionsRequises
    ) {
        public TacheInterne(String titre, String description, String echeance, String priorite, String responsableId, String createurId, Origine origine) {
            this(titre, description, echeance, priorite, responsableId, createurId, origine, null, null, null);
        }

        public TacheInterne(
            String titre,
            String description,
            String echeance,
            String priorite,
            String responsableId,
            String createurId,
            Origine origine,
            List<String> profilsRequis,
            String uniteCible
        ) {
            this(titre, description, echeance, priorite, responsableId, createurId, origine, profilsRequis, uniteCible, null);
        }
    }

    record TacheLue(Tache tache, List<TacheHistorique> historique, Map<String, String> noms, boolean enRetard) {}

    /** Désignation par un autre service des tâches qu'il a créées : origine (type facultatif, référence) et responsable facultatif. */
    record CibleInterne(Origine origine, String responsableId, boolean aPrendre) {
        public CibleInterne(Origine origine, String responsableId) {
            this(origine, responsableId, false);
        }
    }

    /** {@code creee} faux : la tâche existait déjà pour cette origine et ce responsable (idempotence). */
    record Creation(TacheLue tache, boolean creee) {}

    /**
     * {@code vue} : moi (responsable, et tâches à prendre que l'appelant peut prendre par ses profils, D-93), creees (confiées par moi), equipe (PIL_TEAM_READ et TACHE_SUIVRE),
     * unite (tâches de mes unités, à prendre par défaut).
     */
    Page<TacheLue> taches(Appelant appelant, String vue, String etat, Integer page, Integer taille);

    TacheLue tache(Appelant appelant, UUID id);

    TacheLue creer(Appelant appelant, TacheSaisie saisie);

    /** Une tâche par personne de {@code saisie.responsables()} (2 à 50), liées par un groupe. */
    GroupeLu creerPourPlusieurs(Appelant appelant, TacheSaisie saisie);

    GroupeLu groupe(Appelant appelant, UUID groupeId);

    /**
     * Prise d'une tâche à prendre par un membre de l'unité visée (ou d'une sous-unité), porteur du profil exigé s'il
     * y en a un ; offre d'un autre service : porteur de l'un des profils requis (D-93). Publie pilotage.task-taken.v1.
     */
    TacheLue prendre(Appelant appelant, UUID id);

    /** Attribution d'une tâche à prendre à un membre de l'unité, par le responsable de l'unité ou le créateur. */
    TacheLue attribuer(Appelant appelant, UUID id, String responsableId);

    /** Relance du responsable (créateur, ou TACHE_SUIVRE le couvrant), une par 24 h. */
    TacheLue relancer(Appelant appelant, UUID id);

    TacheLue modifier(Appelant appelant, UUID id, TacheSaisie saisie);

    TacheLue transiter(Appelant appelant, UUID id, String etat, String motif);

    Creation creerPourService(String client, TacheInterne demande);

    /** Tâches d'une origine terminées par le service qui les a créées (D-77) ; idempotent. Rend les tâches visées. */
    List<TacheLue> terminerPourService(String client, CibleInterne cible);

    /** Tâches d'une origine rouvertes par le service qui les a créées (D-77) ; idempotent. Rend les tâches visées. */
    List<TacheLue> rouvrirPourService(String client, CibleInterne cible);
}
