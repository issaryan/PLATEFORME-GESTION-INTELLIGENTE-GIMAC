package cm.gimac.des.homologation.domain.parcours;

import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur qui agit : sujet et nom du jeton, permissions (matrice des rôles, D-61) et institution
 * du jeton pour le portail (RG-59).
 */
public record Acteur(UUID id, String nom, Set<String> permissions, String institutionId) {
    public Acteur {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean peut(String... unePermission) {
        for (String p : unePermission) {
            if (permissions.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si l'acteur est la personne désignée. */
    public boolean est(Personne p) {
        return p != null && id != null && id.toString().equals(p.id());
    }
}
