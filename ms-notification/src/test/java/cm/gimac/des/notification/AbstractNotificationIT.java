package cm.gimac.des.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.notification.infrastructure.client.AnnuaireStubAdapter;
import cm.gimac.des.notification.infrastructure.client.ConversationsStubAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base des tests d'intégration des notifications (D-80) : PostgreSQL, Valkey et Kafka par Testcontainers,
 * adaptateurs de substitution (annuaire, conversations), jeton simulé à deux facteurs.
 */
@IntegrationTest
@AutoConfigureMockMvc
public abstract class AbstractNotificationIT {

    /** Permission de la boîte de notifications (§5) : tous les profils internes. */
    public static final String BOITE = "NOT_INBOX_READ";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected AnnuaireStubAdapter annuaire;

    @Autowired
    protected ConversationsStubAdapter conversations;

    @AfterEach
    void effacerDonneesDuScenario() {
        annuaire.reinitialiser();
        conversations.reinitialiser();
        jdbc.execute((ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table notification_utilisateur, preference_notification, outbox_event, processed_event cascade");
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

    /** Compte interne portant NOT_INBOX_READ. */
    public static RequestPostProcessor interne(String id) {
        return utilisateur(id, BOITE);
    }

    protected JsonNode lire(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected int compter(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
