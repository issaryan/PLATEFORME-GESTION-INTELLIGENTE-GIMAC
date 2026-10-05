package cm.gimac.des.pilotage;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.client.DocumentStubAdapter;
import cm.gimac.des.pilotage.infrastructure.client.OrganisationStubAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
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
 * Base des tests d'intégration de Mon activité (D-76) : PostgreSQL, Valkey et Kafka par Testcontainers,
 * adaptateurs de substitution (organisation, documents), jeton simulé à deux facteurs.
 */
@IntegrationTest
@AutoConfigureMockMvc
public abstract class AbstractPilotageIT {

    /** Permissions d'un profil interne (docs/lot1/mon-activite.md §4). */
    public static final String[] AGENT_PERMISSIONS = { "PIL_ACTIVITY_OWN" };
    public static final String[] RESPONSABLE_PERMISSIONS = { "PIL_ACTIVITY_OWN", "PIL_TEAM_READ", "PIL_TASK_ASSIGN" };

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected OrganisationStubAdapter organisation;

    @Autowired
    protected DocumentStubAdapter documents;

    @Autowired
    protected Clock horloge;

    @AfterEach
    void effacerDonneesDuScenario() {
        organisation.reinitialiser();
        jdbc.execute((ConnectionCallback<Void>) c -> {
            try (var st = c.createStatement()) {
                st.execute("truncate table evenement_participant, evenement_agenda, activite, tache_historique, tache, rapport, outbox_event cascade");
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

    public static RequestPostProcessor agent(String id) {
        return utilisateur(id, AGENT_PERMISSIONS);
    }

    public static RequestPostProcessor responsable(String id) {
        return utilisateur(id, RESPONSABLE_PERMISSIONS);
    }

    /** Jeton de service Keycloak (client_credentials) du client donné. */
    public static RequestPostProcessor service(String client, String... permissions) {
        return jwt()
            .jwt(j -> j.subject("sa-" + client).claim("azp", client).claim("preferred_username", "service-account-" + client))
            .authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new));
    }

    protected LocalDate aujourdhui() {
        return ReglesActivite.aujourdhui(horloge);
    }

    protected String corps(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    protected JsonNode lire(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    protected int compter(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
