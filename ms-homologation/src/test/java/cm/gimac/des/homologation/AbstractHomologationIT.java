package cm.gimac.des.homologation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.homologation.config.AsyncSyncConfiguration;
import cm.gimac.des.homologation.config.DatabaseTestcontainer;
import cm.gimac.des.homologation.config.JacksonHibernateConfiguration;
import cm.gimac.des.homologation.config.KafkaTestContainer;
import cm.gimac.des.homologation.config.TestSecurityConfiguration;
import java.util.Arrays;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration du lot 1 (PI-GIMAC-01, HOM-01 C) : PostgreSQL et Kafka par
 * Testcontainers, adaptateurs de substitution, jeton simulé portant les permissions voulues.
 */
@SpringBootTest(
    classes = {
        HomologationApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractHomologationIT {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected HomologationFixtures fixtures;

    @Autowired
    private RelaisOutboxDeTest relais;

    /**
     * Les scénarios du lot passent par MockMvc et valident leurs transactions : leurs données sont
     * effacées après chaque test pour ne pas fausser les tests générés, qui comptent les lignes.
     */
    @org.junit.jupiter.api.AfterEach
    void effacerDonneesDuScenario() {
        effacer(jdbc);
    }

    /** La réserve de connexions n'est pas en validation automatique (JHipster) : validation explicite. */
    public static void effacer(JdbcTemplate jdbc) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table liaison, referential_version, outbox_event cascade");
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    protected void relayerOutbox() {
        relais.relayer();
    }

    protected void relayerOutboxJusquaEnvoi(String aggregateId) throws InterruptedException {
        relais.relayerJusquaEnvoi(aggregateId);
    }

    protected static RequestPostProcessor utilisateur(UUID id, String... permissions) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("acr", "2")).authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    protected Consumer<String, String> consommateur(String sujet) {
        return relais.consommateur(sujet);
    }
}
