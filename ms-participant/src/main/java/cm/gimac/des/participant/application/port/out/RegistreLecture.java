package cm.gimac.des.participant.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lecture seule du registre officiel pour la Sandbox GIMAC (US-EXI-21, D-120) : identité d'une institution par son code
 * participant, correspondants actifs, nomenclature des services souscrits. Aucune écriture.
 */
public interface RegistreLecture {
    /** Identité d'une institution au registre. */
    record Identite(UUID id, String codeParticipant, String raisonSociale, String statut) {}

    /** Correspondant actif : identifiant au registre, compte de la plateforme (MS-IAM) s'il en a un, nom et fonction. */
    record Correspondant(UUID id, UUID compteId, String nom, String fonction) {}

    /** Code de service de la nomenclature et nombre d'institutions qui le souscrivent (abonnement actif). */
    record Service(String code, long institutions) {}

    Optional<Identite> parCode(String codeParticipant);

    /** Identité d'une institution par son identifiant au registre (D-134 : traduction de institution_id par MS-IAM). */
    Optional<Identite> parId(UUID id);

    List<Correspondant> correspondantsActifs(UUID institutionId);

    /** Codes des services souscrits par l'institution (abonnements actifs), triés (CC-SBX-GIMAC-02 algorithme 6.7). */
    List<String> servicesSouscrits(UUID institutionId);

    /** Membre du registre doté d'un code participant (D-140). */
    record Membre(String codeParticipant, String statut) {}

    /** Institutions dotées d'un code participant, filtrées par statut (aucun filtre si vide), une page triée par code. */
    List<Membre> membres(java.util.Set<String> statuts, int page, int taille);

    long nombreMembres(java.util.Set<String> statuts);

    /** Codes de service actifs, une page triée par code. */
    List<Service> services(int page, int taille);

    long nombreServices();
}
