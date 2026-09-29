package cm.gimac.des.iam.domain.compte;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Compte d'un utilisateur de la plateforme, tel que le fournisseur d'identité (Keycloak, autorité
 * des identités et des facteurs) le décrit. Aucun mot de passe ni secret n'y figure (RG-65).
 *
 * @param profilComplet vrai quand l'utilisateur a terminé sa configuration de premier accès
 */
public record Compte(
    String id,
    String identifiant,
    String prenom,
    String nom,
    String email,
    boolean emailVerifie,
    boolean actif,
    Instant creeLe,
    String telephone,
    String fonction,
    String langue,
    String institutionId,
    boolean profilComplet,
    Set<String> profils,
    List<ActionRequise> actionsRequises,
    boolean secondFacteur
) {
    public Compte {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
        actionsRequises = actionsRequises == null ? List.of() : List.copyOf(actionsRequises);
    }

    public String nomAffiche() {
        String n = ((prenom == null ? "" : prenom) + " " + (nom == null ? "" : nom)).trim();
        return n.isEmpty() ? identifiant : n;
    }

    /** Le premier accès n'est pas terminé tant qu'une action requise ou la fiche reste à faire. */
    public boolean premierAccesEnAttente() {
        return !profilComplet || !actionsRequises.isEmpty();
    }
}
