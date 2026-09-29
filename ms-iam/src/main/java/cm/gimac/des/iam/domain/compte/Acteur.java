package cm.gimac.des.iam.domain.compte;

import java.util.Set;

/** Acteur authentifié : sujet du jeton, nom affiché et permissions portées par le jeton. */
public record Acteur(String id, String nom, Set<String> permissions) {
    public Acteur {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean peut(String permission) {
        return permissions.contains(permission);
    }
}
