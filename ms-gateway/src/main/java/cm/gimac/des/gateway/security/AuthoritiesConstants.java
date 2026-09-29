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

    private AuthoritiesConstants() {}
}
