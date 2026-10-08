package cm.gimac.des.audit.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.IntegrationTest;
import cm.gimac.des.audit.application.port.in.ScellerSignatureUseCase.SignatureRecue;
import cm.gimac.des.audit.application.usecase.RegistreAuditService;
import cm.gimac.des.audit.config.KafkaTestContainer;
import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.domain.registre.ArbreMerkle;
import cm.gimac.des.audit.domain.registre.NatureSignature;
import cm.gimac.des.audit.repository.AuditEventRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Scellement des signatures au registre d'audit sur PostgreSQL et Kafka réels (D-93 §11) : index sans trou, feuille
 * de la forme canonique, nœuds des sous-arbres complets d'où la racine se recalcule, relivraison sans effet,
 * consommation des sujets pilotage.process et homologation.operation.
 */
@IntegrationTest
@Import(KafkaTestContainer.class)
@TestPropertySource(
    properties = {
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "gimac.kafka.rebut.actif=true",
        "gimac.kafka.rebut.topics-surveilles=pilotage.process,homologation.operation",
    }
)
class SignaturesRegistreIT {

    @Autowired
    RegistreAuditService registre;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AuditEventRepository evenements;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    KafkaListenerEndpointRegistry consommateurs;

    @Autowired
    KafkaContainer kafka;

    @BeforeEach
    void avant() {
        viderRegistre();
    }

    /** Registre vidé pour les classes suivantes (les entrées sont validées par le service). */
    @AfterEach
    void apres() {
        viderRegistre();
    }

    void viderRegistre() {
        var c = consommateurs.getListenerContainer(SignaturesConsumer.GROUPE);
        if (c != null && c.isRunning()) {
            c.stop();
        }
        // Connexions sans validation automatique (Hikari) : suppression dans une transaction explicite.
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            jdbc.execute("delete from merkle_node");
            jdbc.execute("delete from deduplication_entry");
            jdbc.execute("delete from audit_event");
        });
    }

    static String enveloppe(String id, String type, String signataire) {
        return (
            "{\"specversion\":\"1.0\",\"id\":\"" + id + "\",\"type\":\"" + type + "\",\"source\":\"/ms-pilotage\",\"time\":\"2026-10-07T08:00:00Z\"," +
            "\"data\":{\"signataireId\":\"" + signataire + "\",\"empreinte\":\"" + "ab".repeat(32) + "\",\"documents\":[{\"livrable\":\"PV\",\"documentId\":\"d-1\",\"sha256\":\"cd\"}]}}"
        );
    }

    int compter(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("Cinq signatures : index 0 à 4, feuille de la forme canonique, racine recalculée depuis les nœuds, relivraison sans effet")
    void scellement() {
        for (int i = 0; i < 5; i++) {
            String id = UUID.randomUUID().toString();
            String m = enveloppe(id, NatureSignature.PROCESSUS.type(), "s-" + i);
            assertThat(registre.sceller(new SignatureRecue(id, NatureSignature.PROCESSUS, Instant.parse("2026-10-07T08:00:00Z"), null, "s-" + i, m))).contains((long) i);
            assertThat(registre.sceller(new SignatureRecue(id, NatureSignature.PROCESSUS, null, null, "s-" + i, m))).isEmpty();
        }
        assertThat(compter("select count(*) from audit_event")).isEqualTo(5);
        assertThat(compter("select count(*) from deduplication_entry")).isEqualTo(5);
        // 5 feuilles, 2 nœuds de niveau 1, 1 de niveau 2.
        assertThat(compter("select count(*) from merkle_node")).isEqualTo(8);
        // Lecture du contenu (colonne LOB) dans une transaction en lecture seule.
        List<AuditEvent> inscrits = new TransactionTemplate(transactions).execute(t -> {
            List<AuditEvent> l = evenements.findAll(Sort.by("logIndex"));
            l.forEach(e -> e.getPayload().length());
            return l;
        });
        assertThat(inscrits).extracting(AuditEvent::getLogIndex).containsExactly(0L, 1L, 2L, 3L, 4L);
        List<String> feuilles = inscrits.stream().map(AuditEvent::getLeafHash).toList();
        inscrits.forEach(e -> {
            assertThat(ArbreMerkle.feuille(e.getPayload())).isEqualTo(e.getLeafHash());
            // Forme canonique RFC 8785 : clés triées, sans espace.
            assertThat(e.getPayload()).startsWith("{\"data\":{\"documents\":");
        });
        List<Map<String, Object>> lignes = jdbc.queryForList("select actor_id, action_type, domain, subject from audit_event order by log_index");
        assertThat(lignes.get(2)).containsEntry("actor_id", "s-2").containsEntry("action_type", "SIGNATURE_PROCESSUS").containsEntry("domain", "pilotage").containsEntry("subject", "pilotage.process-step-signed.v1");
        assertThat(registre.racine(5)).isEqualTo(ArbreMerkle.racine(feuilles));
        assertThat(registre.racine(4)).isEqualTo(ArbreMerkle.racine(feuilles.subList(0, 4)));
    }

    @Test
    @Timeout(120)
    @DisplayName("Kafka : signature de processus et d'opération scellées, doublon consommé une fois, autres types ignorés")
    void kafka() throws Exception {
        String p = UUID.randomUUID().toString();
        String o = UUID.randomUUID().toString();
        String processus = enveloppe(p, "pilotage.process-step-signed.v1", "chef");
        String operation =
            "{\"specversion\":\"1.0\",\"id\":\"" + o + "\",\"type\":\"homologation.operation-signed.v1\",\"source\":\"/gimac/des/homologation\"," +
            "\"correlationid\":\"" + UUID.randomUUID() + "\",\"data\":{\"liaisonId\":\"l-1\",\"signataireId\":\"ingenieur\",\"empreinte\":\"" + "ef".repeat(32) + "\",\"le\":\"2026-10-07T09:00:00Z\"}}";
        String autre = "{\"specversion\":\"1.0\",\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"pilotage.process-step-activated.v1\",\"data\":{}}";
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (var producteur = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
            producteur.send(new ProducerRecord<>("pilotage.process", "i-1", processus)).get();
            producteur.send(new ProducerRecord<>("pilotage.process", "i-1", autre)).get();
            producteur.send(new ProducerRecord<>("pilotage.process", "i-1", processus)).get();
            producteur.send(new ProducerRecord<>("homologation.operation", "l-1", operation)).get();
        }
        consommateurs.getListenerContainer(SignaturesConsumer.GROUPE).start();
        int n = 0;
        for (int essai = 0; essai < 120 && n < 2; essai++) {
            n = compter("select count(*) from audit_event");
            if (n < 2) {
                Thread.sleep(500);
            }
        }
        Thread.sleep(1500);
        assertThat(compter("select count(*) from audit_event")).isEqualTo(2);
        assertThat(compter("select count(*) from audit_event where event_id = ?::uuid and action_type = 'SIGNATURE_PROCESSUS' and actor_id = 'chef'", p)).isEqualTo(1);
        assertThat(
            compter("select count(*) from audit_event where event_id = ?::uuid and action_type = 'SIGNATURE_OPERATION' and domain = 'homologation' and actor_id = 'ingenieur' and correlation_id is not null", o)
        ).isEqualTo(1);
        assertThat(compter("select count(*) from merkle_node")).isEqualTo(3);
        assertThat(compter("select max(log_index) from audit_event")).isEqualTo(1);
    }
}
