package cm.gimac.des.certification.application.port.in;

import java.util.Set;

/**
 * Utilisateur à l'origine d'une requête : sujet du jeton, nom affiché, permissions de la matrice des rôles
 * et institution (claim institution_id, portail seulement).
 */
public record Acteur(String id, String nom, Set<String> permissions, String institutionId) {
    public Acteur {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
