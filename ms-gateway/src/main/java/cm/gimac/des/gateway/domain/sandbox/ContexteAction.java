package cm.gimac.des.gateway.domain.sandbox;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Contexte d'action d'un compte d'institution à double rôle (D-133) : une même personne administre son institution
 * (ROLE_SBX_ADMIN_INSTITUTION) et l'intègre (ROLE_SBX_DEVELOPPEUR). Le contexte, choisi par l'en-tête
 * {@value #ENTETE} et INTEGRATION par défaut (moindre privilège), réduit les permissions du jeton relayé :
 * <ul>
 * <li>ADMINISTRATION : permissions du module HAB et consultations seulement ;</li>
 * <li>INTEGRATION : toutes les permissions sauf SBX_HAB_ADMINISTRATION.</li>
 * </ul>
 * Un compte sans double rôle n'est pas réduit. Une valeur inconnue de l'en-tête est refusée. Le contexte retenu est porté
 * par le jeton réduit (claim {@code contexte}), donc par chaque acte tracé des services.
 */
public enum ContexteAction {
    ADMINISTRATION,
    INTEGRATION;

    public static final String ENTETE = "X-Contexte-Sandbox";
    static final String ADMINISTRER = "SBX_HAB_ADMINISTRATION";
    static final String PREFIXE_HAB = "SBX_HAB_";
    static final String CONSULTATION = "_CONSULTATION";

    /** Double rôle : administration de l'institution et au moins une action d'un autre module. */
    public static boolean doubleRole(List<String> permissions) {
        return permissions.contains(ADMINISTRER) && permissions.stream().anyMatch(p -> p.startsWith("SBX_") && !p.startsWith(PREFIXE_HAB) && !p.endsWith(CONSULTATION));
    }

    /** Contexte demandé ; absent : INTEGRATION ; inconnu : vide (refus). */
    public static Optional<ContexteAction> lire(String entete) {
        if (entete == null || entete.isBlank()) {
            return Optional.of(INTEGRATION);
        }
        try {
            return Optional.of(valueOf(entete.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Permissions du contexte. */
    public List<String> reduire(List<String> permissions) {
        return switch (this) {
            case ADMINISTRATION -> permissions.stream().filter(p -> p.startsWith(PREFIXE_HAB) || p.endsWith(CONSULTATION)).toList();
            case INTEGRATION -> permissions.stream().filter(p -> !ADMINISTRER.equals(p)).toList();
        };
    }
}
