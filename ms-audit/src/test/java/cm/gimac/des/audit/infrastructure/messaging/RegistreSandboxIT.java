package cm.gimac.des.audit.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.application.usecase.RegistreAuditService;
import cm.gimac.des.audit.config.KafkaTestContainer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

/**
 * US-EXI-10 sur PostgreSQL et Kafka réels : événements à effet de la sandbox inscrits au registre (domaine sandbox,
 * institution), redélivrance sans double inscription, enveloppe non conforme au rebut avec son motif, recherche par
 * source et institution, export CSV, abonnement par motif aux sujets sbx-*.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Import(KafkaTestContainer.class)
@TestPropertySource(
    properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.properties.metadata.max.age.ms=1000",
        "gimac.audit.sandbox.seuil-rebut=1",
    }
)
class RegistreSandboxIT {

    @Autowired
    RegistreAuditService registre;

    @Autowired
    EvenementsSandboxConsumer consommateur;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    KafkaContainer kafka;

    @Autowired
    MockMvc mvc;

    @BeforeEach
    @AfterEach
    void vider() {
        var c = consommateurs.getListenerContainer(EvenementsSandboxConsumer.GROUPE);
        if (c != null && c.isRunning()) {
            c.stop();
        }
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            jdbc.execute("delete from merkle_node");
            jdbc.execute("delete from deduplication_entry");
            jdbc.execute("delete from audit_event");
            jdbc.execute("delete from rebut_sandbox");
        });
    }

    static String evenement(String id, String type, String institution, String acteur) {
        String service = type.substring(0, type.indexOf('.'));
        return (
            "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"source\":\"/" + service + "\",\"time\":\"2026-10-22T08:00:00Z\"," +
            (institution == null ? "" : "\"institutionid\":\"" + institution + "\",") +
            "\"data\":{\"acteurId\":\"" + acteur + "\",\"objet\":\"EXE-1\"}}"
        );
    }

    static RequestPostProcessor rssi(String... permissions) {
        return jwt().jwt(j -> j.subject("demo-rssi").claim("acr", "2")).authorities(java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
    }

    int compter(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("Critères 1 à 3 : verdict inscrit (auteur, horodatage, institution, feuille), redélivrance sans effet, recherche par 99001, export CSV, rebut")
    void registreDeLaSandbox() throws Exception {
        String verdict = UUID.randomUUID().toString();
        consommateur.traiter("sbx-campagne.verdict", evenement(verdict, "sbx-campagne.verdict-validated.v1", "99001", "u-certificateur"));
        consommateur.traiter("sbx-campagne.verdict", evenement(verdict, "sbx-campagne.verdict-validated.v1", "99001", "u-certificateur"));
        consommateur.traiter("sbx-raccordement.access", evenement(UUID.randomUUID().toString(), "sbx-raccordement.access-issued.v1", "99002", "u-dev"));
        consommateur.traiter("sbx-trace.trace", evenement("purge-1", "sbx-trace.trace-purged.v1", null, "sbx-trace"));
        consommateur.traiter("sbx-lecture.access", evenement("x-1", "sbx-lecture.access-recorded.v1", "99001", "u").replace("\"time\":\"2026-10-22T08:00:00Z\",", ""));
        consommateur.traiter("sbx-lecture.access", "{illisible");

        assertThat(compter("select count(*) from audit_event where domain = 'sandbox'")).isEqualTo(3);
        assertThat(compter("select count(*) from audit_event where event_id = ?::uuid and actor_id = 'u-certificateur' and institution_id = '99001' and subject = 'sbx-campagne.verdict-validated.v1'", verdict)).isEqualTo(1);
        String feuille = jdbc.queryForObject("select leaf_hash from audit_event where event_id = ?::uuid", String.class, verdict);
        assertThat(feuille).matches("[0-9a-f]{64}");
        assertThat(registre.racine(3)).isNotBlank();
        assertThat(compter("select count(*) from rebut_sandbox")).isEqualTo(2);
        assertThat(compter("select count(*) from rebut_sandbox where motif = 'TIME_INVALIDE' and event_id = 'x-1'")).isEqualTo(1);

        mvc
            .perform(get("/api/v1/registre/sandbox").param("institution", "99001").with(rssi("SBX_TRA_CONSULTATION")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.contenu[0].eventId").value(verdict))
            .andExpect(jsonPath("$.contenu[0].acteur").value("u-certificateur"))
            .andExpect(jsonPath("$.contenu[0].empreinte").value(feuille));
        mvc.perform(get("/api/v1/registre/sandbox").param("taille", "500").with(rssi("SBX_TRA_CONSULTATION"))).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.taille").value(100));
        byte[] csv = mvc
            .perform(get("/api/v1/registre/sandbox/export").param("institution", "99 001/x").with(rssi("SBX_TRA_CONSULTATION")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"registre-sandbox-99_001_x.csv\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        assertThat(new String(csv, StandardCharsets.UTF_8)).isEqualTo("index,identifiant,survenu_le,inscrit_le,acteur,type,institution,empreinte_feuille\r\n");
        String tout = mvc.perform(get("/api/v1/registre/sandbox/export").with(rssi("SBX_TRA_CONSULTATION"))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(tout.split("\r\n")).hasSize(4);
        assertThat(tout).contains("u-certificateur,sbx-campagne.verdict-validated.v1,99001," + feuille);
        mvc
            .perform(get("/api/v1/registre/sandbox/rebut").with(rssi("SBX_TRA_CONSULTATION")))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.seuil").value(1))
            .andExpect(jsonPath("$.alerte").value(true))
            .andExpect(jsonPath("$.contenu[0].motif").value("JSON_ILLISIBLE"));
        mvc.perform(get("/api/v1/registre/sandbox").with(rssi("SBX_HAB_CONSULTATION"))).andExpect(status().isForbidden());
    }

    @Test
    @Timeout(120)
    @DisplayName("Kafka : abonnement par motif aux sujets sbx-*, événement inscrit une fois malgré sa redélivrance, file de rebut .dlq ignorée")
    void kafka() throws Exception {
        String id = UUID.randomUUID().toString();
        String m = evenement(id, "sbx-espace.space-opened.v1", "99001", "demo-admin");
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (var p = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            p.send(new ProducerRecord<>("sbx-espace.space-opened.v1", "99001", m)).get();
            p.send(new ProducerRecord<>("sbx-espace.space-opened.v1", "99001", m)).get();
            p.send(new ProducerRecord<>("sbx-espace.space-opened.v1.dlt", "99001", evenement(UUID.randomUUID().toString(), "sbx-espace.space-opened.v1", "99001", "x"))).get();
        }
        consommateurs.getListenerContainer(EvenementsSandboxConsumer.GROUPE).start();
        for (int essai = 0; essai < 200 && compter("select count(*) from audit_event where domain = 'sandbox'") < 1; essai++) {
            Thread.sleep(500);
        }
        Thread.sleep(1500);
        assertThat(compter("select count(*) from audit_event where domain = 'sandbox'")).isEqualTo(1);
        assertThat(compter("select count(*) from audit_event where event_id = ?::uuid and institution_id = '99001'", id)).isEqualTo(1);
    }
}
