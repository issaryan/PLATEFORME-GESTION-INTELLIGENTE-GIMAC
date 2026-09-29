package cm.gimac.des.iam.domain.compte;

import java.util.Set;

/** Profil par fonction (rôle composite du realm) et les permissions qui le composent (D-58). */
public record Profil(String code, String description, Set<String> permissions, boolean privilegie, long titulaires) {
    public Profil {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
