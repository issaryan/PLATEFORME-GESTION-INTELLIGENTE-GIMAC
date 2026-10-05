package cm.gimac.des.pilotage.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Organisation portée par MS-IAM (docs/lot1/mon-activite.md §1.3), lue avec le jeton du client
 * pilotage-service (INTERNAL_IAM_READ) : rattachement d'un compte, son équipe, un collaborateur.
 * Les erreurs de joignabilité lèvent 503 PIL_ORGANISATION_INDISPONIBLE, sans valeur supposée.
 */
public interface OrganisationPort {
    /** Compte de l'annuaire : un compte interne n'a pas d'institution. */
    record Personne(String id, String nomAffiche, String fonction, boolean actif, boolean interne) {}

    /** GET /api/v1/interne/organisation/{id} : unité la plus fine (code), responsable (n+1), chaîne des responsables. */
    record Rattachement(String id, String nomAffiche, String unite, Personne responsable, List<String> chaine) {}

    /** Élément de GET /api/v1/interne/organisation/{id}/equipe (récursif, 500 au plus). */
    record Membre(String id, String nomAffiche, String fonction, String unite, String responsableId, int niveau, boolean actif) {}

    Optional<Rattachement> rattachement(String compteId);

    /** Comptes dont {@code compteId} figure dans la chaîne des responsables ; vide s'il n'en a aucun. */
    List<Membre> equipe(String compteId);

    Optional<Personne> collaborateur(String compteId);
}
