package cm.gimac.des.notification.application.port.in;

import cm.gimac.des.notification.domain.service.ReglesNotifications.FaitsProcessus;
import java.time.Instant;
import java.util.List;

/**
 * Événements des autres services transformés en notifications (docs/lot1/notifications.md §1) : une
 * notification par destinataire, jamais pour l'auteur ou l'acteur, selon ses préférences ; chaque
 * notification créée ou regroupée publie notification.inapp-created.v1 par l'outbox.
 */
public interface RecevoirEvenementsUseCase {
    /**
     * collaboration.message-sent.v1 (sujet collaboration.message).
     *
     * @param evenementGroupe événement de groupe (D-68) inscrit dans le fil : ignoré, seul un vrai message notifie
     * @param reponseAAuteurId auteur du message cité (D-82) ; null sans réponse, absent des anciens événements
     * @param mentionnes membres mentionnés (D-82), jamais l'auteur ; vide si absent
     */
    record MessageEnvoye(
        String conversationId,
        String messageId,
        String auteurId,
        List<String> destinataires,
        Instant envoyeLe,
        String evenementGroupe,
        String reponseAAuteurId,
        List<String> mentionnes
    ) {
        public MessageEnvoye {
            mentionnes = mentionnes == null ? List.of() : List.copyOf(mentionnes);
        }

        /** Message sans réponse ni mention (événements antérieurs à D-82). */
        public MessageEnvoye(String conversationId, String messageId, String auteurId, List<String> destinataires, Instant envoyeLe, String evenementGroupe) {
            this(conversationId, messageId, auteurId, destinataires, envoyeLe, evenementGroupe, null, List.of());
        }
    }

    /** pilotage.task-assigned.v1 (sujet pilotage.task) ; titre et échéance facultatifs (absents de la charge v1). */
    record TacheAffectee(String tacheId, String responsableId, String acteurId, Instant le, String titre, String echeance) {}

    /** pilotage.report-submitted.v1 (sujet pilotage.report). */
    record RapportSoumis(String rapportId, String type, String periode, String auteurId, String destinataireId, String acteurId, Instant le) {}

    /**
     * pilotage.task-offered.v1 (sujet pilotage.task, docs/lot1/pouvoirs-hierarchiques.md §6) : tâche à
     * prendre par les membres d'une unité. {@code lien} (D-98, facultatif) : écran où l'acte se fait sans prise préalable
     * (offre d'origine DECISION : prononcer une transition de statut) ; absent : les tâches à prendre.
     */
    record TacheOfferte(String tacheId, String titre, String uniteCible, List<String> destinataires, String acteurId, Instant le, String lien) {
        public TacheOfferte(String tacheId, String titre, String uniteCible, List<String> destinataires, String acteurId, Instant le) {
            this(tacheId, titre, uniteCible, destinataires, acteurId, le, null);
        }
    }

    /** pilotage.task-reminded.v1 (sujet pilotage.task) : relance d'une tâche à son responsable. */
    record TacheRelancee(String tacheId, String titre, String responsableId, String acteurId, Instant le) {}

    /**
     * iam.request-submitted.v1 (sujet iam.request, docs/lot1/pouvoirs-hierarchiques.md §4) : demande à
     * l'administration ; destinataires : comptes qui peuvent la traiter.
     */
    record DemandeSoumise(String demandeId, String type, String demandeurId, List<String> destinataires, Instant le) {}

    /** iam.request-decided.v1 (sujet iam.request) : demande acceptée ou refusée ; destinataire : le demandeur. */
    record DemandeDecidee(String demandeId, String type, String etat, String acteurId, List<String> destinataires, Instant le) {}

    /**
     * Événement à destinataires du sujet pilotage.process (D-87, docs/lot1/processus-unite.md §5.5) : instance
     * lancée, étape activée, soumise, signée ou réaffectée, instance close ou annulée.
     *
     * @param acteurId qui a agi (lanceur, exécutant, signataire, chef), jamais notifié ; null pour une activation
     *     ou un départ
     * @param ancienId personne partie (réaffectation après un départ), null sinon
     * @param lien lien de l'événement, rendu tel quel s'il est sous /des/processus/
     */
    record EvenementProcessus(String instanceId, FaitsProcessus faits, String acteurId, String ancienId, List<String> destinataires, String lien, Instant le) {
        public EvenementProcessus {
            destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
        }
    }

    /**
     * participant.transition-approved.v1 (définitive) et participant.transition-rejected.v1 (sujet participant.transition,
     * DDR-08) : la proposition de transition de statut est prononcée ou refusée ; destinataire : son proposant
     * ({@code demandeurId}), jamais l'approbateur. {@code decision} : APPROUVEE ou REJETEE ; {@code motif} : motif du refus.
     */
    record TransitionTranchee(
        String institutionId,
        String transitionId,
        String institutionNom,
        String vers,
        String decision,
        String motif,
        String demandeurId,
        String acteurId,
        String lien,
        Instant le
    ) {}

    /** Rend le nombre de notifications créées ou regroupées. */
    int messageEnvoye(MessageEnvoye e);

    int tacheAffectee(TacheAffectee e);

    int rapportSoumis(RapportSoumis e);

    int tacheOfferte(TacheOfferte e);

    int tacheRelancee(TacheRelancee e);

    int demandeSoumise(DemandeSoumise e);

    int demandeDecidee(DemandeDecidee e);

    /** Une notification DEMANDE au proposant d'une transition tranchée (DDR-08), jamais regroupée. */
    int transitionTranchee(TransitionTranchee e);

    /** Une notification PROCESSUS par destinataire, jamais regroupée (D-87). */
    int evenementProcessus(EvenementProcessus e);

    /** Purge des notifications de plus de 90 jours (§2) ; rend le nombre supprimé. */
    int purger();
}
