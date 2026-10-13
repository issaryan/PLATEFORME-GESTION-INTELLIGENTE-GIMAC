package cm.gimac.des.homologation.application.port.in;

import cm.gimac.des.homologation.domain.parcours.ValeurReference;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reprise des projets de certification existants (D-107, docs/exploitation/reprise-des-donnees.md §4) : import
 * idempotent des liaisons par référence d'origine, institution désignée par son code membre, avancement repris
 * opération par opération (ou à qualifier, ou en production), projection recalculée par le moteur. En simulation, tout
 * est contrôlé et rien n'est écrit.
 */
public interface RepriseLiaisonsUseCase {
    /** Nombre maximal de liaisons d'un lot. */
    int LIGNES_MAX = 500;

    /** Acteur de la reprise : compte de service reprise-service ou super administrateur. */
    record Acteur(UUID id, String nom) {}

    /** Service du périmètre : code, et pour une liaison avancée, date de certification, ouverture, commentaire. */
    record ServiceRepris(String code, LocalDate certifieLe, Boolean ouvert, String commentaire) {}

    /** Ressource désignée : fonction interne du catalogue (D-91) et compte qui la tient. */
    record RessourceReprise(String fonction, UUID compteId) {}

    /** État repris d'une opération du référentiel publié (rang = code de l'opération). */
    record OperationReprise(
        String rang,
        String etat,
        LocalDate debut,
        LocalDate faiteLe,
        String commentaire,
        ValeurReference valeurReference,
        String documentRef,
        String empreinte,
        LocalDate echeanceLevee
    ) {}

    /**
     * Liaison reprise. {@code referenceOrigine}, {@code codeMembre} et {@code typeProjet} sont obligatoires ;
     * {@code avancement} vaut A_QUALIFIER, DETAILLE ou EN_PRODUCTION (par défaut : DETAILLE s'il y a des opérations,
     * sinon A_QUALIFIER) ; {@code origineEtat} dit d'où vient l'état repris.
     */
    record LiaisonReprise(
        String referenceOrigine,
        String codeMembre,
        String typeProjet,
        String intitule,
        String origineEtat,
        LocalDate dateOuverture,
        LocalDate dateCibleProduction,
        LocalDate dateMiseEnProduction,
        UUID conducteurId,
        List<RessourceReprise> ressources,
        String banquePartenaire,
        List<ServiceRepris> services,
        String avancement,
        List<OperationReprise> operations
    ) {}

    enum Resultat {
        CREEE,
        MISE_A_JOUR,
        INCHANGEE,
        REFUSEE,
    }

    /** Ligne du rapport : liaison, référence lisible, projection recalculée ; code et motif d'un refus. */
    record Ligne(
        int rang,
        String referenceOrigine,
        Resultat resultat,
        UUID liaisonId,
        String reference,
        String statut,
        String etapeCourante,
        String etat,
        int operationsModifiees,
        List<String> avertissements,
        String code,
        String motif,
        Map<String, Object> details
    ) {}

    record Rapport(boolean simulation, int total, Map<Resultat, Long> totaux, List<Ligne> lignes) {}

    Rapport importer(List<LiaisonReprise> lignes, boolean simulation, Acteur acteur);
}
