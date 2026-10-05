package cm.gimac.des.gateway.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    /** Supervision de la plateforme (D-61) : état des services et des serveurs, alertes, flux temps réel. */
    public static final String PLATFORM_SUPERVISION_READ = "PLATFORM_SUPERVISION_READ";

    /** Supervision de la plateforme : configuration effective et niveaux de journalisation, en lecture. */
    public static final String PLATFORM_CONFIG_READ = "PLATFORM_CONFIG_READ";

    /** Supervision de la plateforme : changement à chaud des niveaux de journalisation. */
    public static final String PLATFORM_CONFIG_EDIT = "PLATFORM_CONFIG_EDIT";

    /** Exploitation de l'infrastructure (D-78) : charges de travail, journaux récents, historique des opérations. */
    public static final String PLATFORM_OPS_READ = "PLATFORM_OPS_READ";

    /** Exploitation de l'infrastructure (D-78) : redémarrer, arrêter, démarrer, mettre à l'échelle (avec FIDO2). */
    public static final String PLATFORM_OPS_EXECUTE = "PLATFORM_OPS_EXECUTE";

    private AuthoritiesConstants() {}
}
