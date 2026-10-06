package cm.gimac.des.collaboration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.collaboration.config.AsyncSyncConfiguration;
import cm.gimac.des.collaboration.config.DatabaseTestcontainer;
import cm.gimac.des.collaboration.config.JacksonHibernateConfiguration;
import cm.gimac.des.collaboration.config.KafkaTestContainer;
import cm.gimac.des.collaboration.config.TestSecurityConfiguration;
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
 * Base des tests d'intégration de la messagerie (D-64) : PostgreSQL et Kafka par Testcontainers,
 * adaptateurs de substitution (annuaire, dépôt documentaire), jeton simulé à deux facteurs.
 */
@SpringBootTest(
    classes = {
        CollaborationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractCollaborationIT {

    /** Permissions des profils internes du domaine gimac (infra/keycloak/generer-realm-gimac.py). */
    public static final String[] MESSAGERIE = { "COL_MESSAGE_READ", "COL_MESSAGE_SEND" };

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelaisOutboxDeTest relais;

    /**
     * Les scénarios passent par MockMvc et valident leurs transactions : leurs données sont effacées
     * après chaque test pour ne pas fausser les tests générés, qui comptent les lignes.
     */
    @org.junit.jupiter.api.AfterEach
    void effacerDonneesDuScenario() {
        effacer(jdbc);
    }

    /** La réserve de connexions n'est pas en validation automatique (JHipster) : validation explicite. */
    public static void effacer(JdbcTemplate jdbc) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table piece_jointe, message_etiquette, message_mention, message, conversation_lecture, conversation_membre, conversation, outbox_event cascade");
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    /** Jeton utilisateur à deux facteurs (acr 2) portant les permissions voulues. */
    public static RequestPostProcessor utilisateur(String id, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(id).claim("acr", "2"))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }
}
