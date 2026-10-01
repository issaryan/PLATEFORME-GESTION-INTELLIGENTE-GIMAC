package cm.gimac.des.collaboration.domain.messagerie;

import java.util.Set;

/**
 * Personne de l'annuaire de MS-IAM (D-62) : seuls les comptes internes actifs sont joignables.
 * « avatarVersion » : version de la photo de profil, nulle sans photo (docs/lot1/mon-compte.md §1).
 */
public record Collaborateur(String id, String nom, String fonction, Set<String> profils, String institutionId, boolean actif, String avatarVersion) {
    public Collaborateur {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
    }

    public Collaborateur(String id, String nom, String fonction, Set<String> profils, String institutionId, boolean actif) {
        this(id, nom, fonction, profils, institutionId, actif, null);
    }

    /** Compte interne au GIMAC (sans institution) et actif. */
    public boolean joignable() {
        return actif && (institutionId == null || institutionId.isBlank());
    }
}
