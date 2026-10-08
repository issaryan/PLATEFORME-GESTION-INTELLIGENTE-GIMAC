package cm.gimac.des.participant.application.port.in;

import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Étapes 1 et 2 du parcours (CR-CERT §4.2.1, D-57) et cycle de vie de l'institution
 * (PAR-01 à PAR-04, algorithme 6.25).
 */
public interface ManageInstitutionUseCase {
    /** Acteur authentifié : identifiant (sujet du jeton) et nom affiché. */
    record Acteur(UUID id, String nom) {}

    /** Prise de contact (rang 1) : identité du candidat et projet envisagé. */
    record Candidature(
        String nom,
        String sigle,
        String pays,
        InstitutionCategory categorie,
        String codeIdentification,
        String codeBanqueBeac,
        LocalDate datePriseDeContact,
        String originePriseDeContact,
        String projetEnvisage
    ) {}

    /** Conditions de l'adhésion, arrêtées par la DDRM avant la proposition (rang 2). */
    record ConditionsAdhesion(
        String codeParticipant,
        MembershipMode mode,
        UUID banqueReglementId,
        UUID banqueMarraineId,
        String compteReglement,
        String compteCantonnement
    ) {}

    /** Dépôt d'une pièce : référence MS-DOCUMENT et empreinte SHA-256, jamais le contenu. */
    record Depot(UUID documentId, String documentSha256, String nomFichier) {}

    Institution enregistrerCandidat(Candidature candidature, Acteur acteur);

    Institution modifierFiche(UUID institutionId, Candidature fiche, Acteur acteur);

    Institution definirConditionsAdhesion(UUID institutionId, ConditionsAdhesion conditions, Acteur acteur);

    AdhesionPiece deposerPiece(UUID institutionId, String code, Depot depot, Acteur acteur);

    AdhesionPiece validerPiece(UUID institutionId, String code, Acteur acteur);

    AdhesionPiece rejeterPiece(UUID institutionId, String code, String motif, Acteur acteur);

    List<Prerequis> prerequis(UUID institutionId, InstitutionStatus cible);

    StatusTransition proposerTransition(UUID institutionId, InstitutionStatus vers, String motif, UUID pieceDecision, Acteur acteur);

    StatusTransition approuverTransition(UUID institutionId, UUID transitionId, Acteur acteur, String assertionReauth);

    StatusTransition rejeterTransition(UUID institutionId, UUID transitionId, String motif, Acteur acteur);

    /**
     * Ouverture d'une liaison par MS-HOMOLOGATION (étape 3, D-57) : un adhérent passe
     * automatiquement à EN_HOMOLOGATION ; une institution déjà en homologation reste en l'état.
     *
     * @return la transition automatique enregistrée, vide si aucun changement n'est dû
     */
    java.util.Optional<StatusTransition> debuterHomologation(UUID institutionId, UUID liaisonId, String typeProjet);

    /**
     * Jalon EN_PRODUCTION d'une liaison atteint dans MS-HOMOLOGATION (homologation.liaison-milestone-reached.v1,
     * D-93 §4) : une institution EN_HOMOLOGATION passe automatiquement à ACTIF ; une institution déjà active reste en
     * l'état, tout autre état est signalé sans transition.
     *
     * @return la transition automatique enregistrée, vide si aucun changement n'est dû
     */
    java.util.Optional<StatusTransition> mettreEnProduction(UUID institutionId, UUID liaisonId, String reference, String typeProjet);
}
