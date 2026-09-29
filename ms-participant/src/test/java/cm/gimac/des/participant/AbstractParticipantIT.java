package cm.gimac.des.participant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.participant.config.AsyncSyncConfiguration;
import cm.gimac.des.participant.config.DatabaseTestcontainer;
import cm.gimac.des.participant.config.JacksonHibernateConfiguration;
import cm.gimac.des.participant.config.KafkaTestContainer;
import cm.gimac.des.participant.config.RedisTestContainer;
import cm.gimac.des.participant.config.TestSecurityConfiguration;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration de MS-PARTICIPANT : PostgreSQL, Redis et Kafka par Testcontainers,
 * adaptateurs de substitution, jeton simulé à deux facteurs (acr 2, D-61) portant les permissions
 * voulues. Exécutés sur le poste de développement (D-60).
 */
@SpringBootTest(
    classes = {
        ParticipantApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        JacksonHibernateConfiguration.class,
        DatabaseTestcontainer.class,
        RedisTestContainer.class,
        KafkaTestContainer.class,
    }
)
@AutoConfigureMockMvc
public abstract class AbstractParticipantIT {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected RelaisOutboxDeTest relais;

    /**
     * Les scénarios de la tranche passent par MockMvc et valident leurs transactions : leurs données
     * sont effacées après chaque test pour ne pas fausser les tests générés, qui comptent les lignes.
     */
    @org.junit.jupiter.api.AfterEach
    void effacerDonneesDuScenario() {
        effacer(jdbc);
    }

    /** La réserve de connexions n'est pas en validation automatique (JHipster) : validation explicite. */
    public static void effacer(JdbcTemplate jdbc) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute(
                    "truncate table interaction, status_transition, adhesion_piece, subscribed_service, convention, correspondent, " +
                    "escalation_contact, legal_document, deadline, outbox_event, processed_event, institution cascade"
                );
            }
            if (!c.getAutoCommit()) {
                c.commit();
            }
            return null;
        });
    }

    public static RequestPostProcessor utilisateur(UUID id, String... permissions) {
        return jwt()
            .jwt(j -> j.subject(id.toString()).claim("acr", "2").claim("name", "Agent " + id.toString().substring(0, 8)))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }
}
