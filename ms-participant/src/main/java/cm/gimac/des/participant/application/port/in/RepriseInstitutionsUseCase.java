package cm.gimac.des.participant.application.port.in;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reprise de l'existant (D-107, docs/exploitation/reprise-des-donnees.md §3) : import idempotent des institutions
 * membres par code membre, création ou mise à jour, statut cible atteint par une transition de reprise tracée.
 * En simulation, tout est contrôlé et rien n'est écrit.
 */
public interface RepriseInstitutionsUseCase {
    /** Nombre maximal de lignes d'un lot. */
    int LIGNES_MAX = 2000;

    /** Service souscrit repris : code et date de souscription (à défaut, date d'adhésion). */
    record ServiceRepris(String code, LocalDate depuis) {}

    /**
     * Institution reprise. {@code codeMembre}, {@code nom}, {@code pays}, {@code categorie} et {@code statut} sont
     * obligatoires ; {@code id} garde l'UUID déjà porté par des comptes ; banques de règlement et marraine désignées
     * par leur code membre.
     */
    record InstitutionReprise(
        String codeMembre,
        UUID id,
        String nom,
        String sigle,
        String pays,
        String categorie,
        String codeIdentification,
        String codeBanqueBeac,
        String groupe,
        String statut,
        LocalDate dateDecision,
        String referenceDecision,
        LocalDate datePriseDeContact,
        LocalDate dateAdhesion,
        String modeAdhesion,
        String compteReglement,
        String compteCantonnement,
        String banqueReglement,
        String banqueMarraine,
        List<ServiceRepris> servicesSouscrits
    ) {}

    enum Resultat {
        CREEE,
        MISE_A_JOUR,
        INCHANGEE,
        REFUSEE,
    }

    /** Ligne du rapport : résultat, champs changés, avertissements ; code et motif d'un refus. */
    record Ligne(
        int rang,
        String codeMembre,
        Resultat resultat,
        UUID id,
        String statut,
        List<String> changements,
        List<String> avertissements,
        String code,
        String motif,
        Map<String, Object> details
    ) {}

    record Rapport(boolean simulation, int total, Map<Resultat, Long> totaux, List<Ligne> lignes) {}

    Rapport importer(List<InstitutionReprise> lignes, boolean simulation, String motif, Acteur acteur);
}
