package cm.gimac.des.iam;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.iam.config.AsyncSyncConfiguration;
import cm.gimac.des.iam.config.DatabaseTestcontainer;
import cm.gimac.des.iam.config.JacksonHibernateConfiguration;
import cm.gimac.des.iam.config.KafkaTestContainer;
import cm.gimac.des.iam.config.RedisTestContainer;
import cm.gimac.des.iam.config.TestSecurityConfiguration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration de MS-IAM (D-62) : PostgreSQL, Redis et Kafka par Testcontainers,
 * annuaire en mémoire (l'adaptateur Keycloak a son propre test face au vrai Keycloak), jeton simulé
 * à deux facteurs (acr 2, D-61) portant les permissions voulues.
 */
@SpringBootTest(
    classes = {
        IamApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractIamIT {

    /** Comptes de démonstration de l'annuaire en mémoire. */
    public static final String SUPER_ADMIN = "demo-super-admin";
    public static final String RSSI = "demo-rssi";

    /** Permissions des profils du realm (infra/keycloak/generer-realm-gimac.py). */
    public static final String[] PERMISSIONS_SUPER_ADMIN = {
        "IAM_ACCOUNT_READ",
        "IAM_ACCOUNT_CREATE",
        "IAM_ACCOUNT_EDIT",
        "IAM_ACCOUNT_ROLES",
        "IAM_ACCOUNT_STATUS",
        "IAM_ACCOUNT_DELETE",
        "IAM_ACCOUNT_SECURITY",
        "IAM_JOURNAL_READ",
        "IAM_PROFILE_EDIT",
        "IAM_ROLE_PRIVILEGED_GRANT",
        "PLATFORM_SECURITY_POLICY_READ",
        "PLATFORM_SECURITY_POLICY_EDIT",
        "PLATFORM_MESSAGING_EDIT",
    };
    public static final String[] PERMISSIONS_RSSI = Arrays.stream(PERMISSIONS_SUPER_ADMIN).filter(p -> !p.equals("IAM_ROLE_PRIVILEGED_GRANT")).toArray(String[]::new);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelaisOutboxDeTest relais;

    public static RequestPostProcessor utilisateur(String sujet, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(sujet).claim("acr", "2").claim("name", "Agent " + sujet))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    /**
     * Code TOTP courant pour l'URI otpauth d'un enrôlement : chiffres, période et algorithme repris de
     * l'URI (la politique OTP peut avoir été changée par un autre test du même contexte).
     */
    public static String codeTotp(String uri) {
        java.util.Map<String, String> p = new java.util.HashMap<>();
        for (String kv : uri.substring(uri.indexOf('?') + 1).split("&")) {
            String[] x = kv.split("=", 2);
            p.put(x[0], x[1]);
        }
        return cm.gimac.des.iam.domain.service.Totp.code(p.get("secret"), java.time.Instant.now(), Integer.parseInt(p.get("digits")), Integer.parseInt(p.get("period")), "Hmac" + p.get("algorithm"));
    }

    public static RequestPostProcessor jetonDeService(String... permissions) {
        return jwt().jwt(j -> j.subject("service-account-participant-service")).authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }
}
