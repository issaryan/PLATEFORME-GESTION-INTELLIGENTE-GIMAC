package cm.gimac.des.collaboration.domain.messagerie;

import java.util.Set;

/** Personne de l'annuaire de MS-IAM (D-62) : seuls les comptes internes actifs sont joignables. */
public record Collaborateur(String id, String nom, String fonction, Set<String> profils, String institutionId, boolean actif) {
    public Collaborateur {
        profils = profils == null ? Set.of() : Set.copyOf(profils);
    }

    /** Compte interne au GIMAC (sans institution) et actif. */
    public boolean joignable() {
        return actif && (institutionId == null || institutionId.isBlank());
    }
}
