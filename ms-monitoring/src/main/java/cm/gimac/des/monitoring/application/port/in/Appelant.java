package cm.gimac.des.monitoring.application.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Compte interne appelant : sujet du jeton (identifiant du compte MS-IAM), nom affiché et permissions portées
 * (docs/lot1/astreintes-monitoring.md §2).
 */
public record Appelant(String id, String nom, Set<String> permissions) {
    public static final String LECTURE = "MON_SUPERVISION_READ";
    public static final String ACQUITTER = "MON_ALERT_ACK";
    public static final String OPERATEUR = "MON_SHIFT_OPERATOR";
    public static final String ASTREINTE = "MON_ONCALL_L2";
    public static final String CHEF_DE_QUART = "MON_SHIFT_LEAD";
    public static final String GERER_GRILLE = "MON_ROSTER_MANAGE";
    public static final String PUBLIER_GRILLE = "MON_ROSTER_PUBLISH";

    public Appelant {
        permissions = Set.copyOf(permissions);
    }

    public boolean porte(String permission) {
        return permissions.contains(permission);
    }

    /** Identifiant du compte en UUID (sujet Keycloak) ; vide s'il n'en est pas un. */
    public java.util.Optional<UUID> uuid() {
        try {
            return java.util.Optional.of(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException e) {
            return java.util.Optional.empty();
        }
    }
}
