package cm.gimac.des.certification;

import cm.gimac.des.certification.infrastructure.messaging.outbox.OutboxRelay;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

/**
 * Relais de l'outbox et lecture d'un sujet pour les tests d'intégration et Cucumber du lot 1. La
 * planification est désactivée dans les profils de test générés : le relais est déclenché explicitement ;
 * le premier envoi vers un sujet créé à la volée peut échouer, on relaie donc jusqu'à l'envoi effectif.
 */
@Component
public class RelaisOutboxDeTest {

    private final OutboxRelay relais;
    private final JdbcTemplate jdbc;
    private final KafkaConnectionDetails kafka;

    public RelaisOutboxDeTest(OutboxRelay relais, JdbcTemplate jdbc, KafkaConnectionDetails kafka) {
        this.relais = relais;
        this.jdbc = jdbc;
        this.kafka = kafka;
    }

    public void relayerJusquaEnvoi(String aggregateId) throws InterruptedException {
        for (int essai = 0; essai < 20; essai++) {
            relais.relay();
            Integer restants = jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and status <> 'SENT'",
                Integer.class,
                aggregateId
            );
            if (restants != null && restants == 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Événements de " + aggregateId + " non relayés vers Kafka");
    }

    /** Types des événements écrits dans l'outbox pour cet agrégat, dans l'ordre. */
    public List<String> types(String aggregateId) {
        return jdbc.queryForList("select event_type from outbox_event where aggregate_id = ? order by created_at, event_type", String.class, aggregateId);
    }

    /** Enregistrements du sujet dont la clé est l'agrégat attendu : lecture jusqu'à en avoir {@code attendus} (20 s au plus). */
    public List<ConsumerRecord<String, String>> lire(String sujet, String aggregateId, int attendus) {
        var props = KafkaTestUtils.consumerProps(String.join(",", kafka.getBootstrapServers()), "it-" + UUID.randomUUID(), "true");
        try (
            Consumer<String, String> c = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer()).createConsumer()
        ) {
            c.subscribe(List.of(sujet));
            List<ConsumerRecord<String, String>> liste = new ArrayList<>();
            long fin = System.currentTimeMillis() + 20_000;
            while (liste.size() < attendus && System.currentTimeMillis() < fin) {
                c.poll(Duration.ofMillis(500)).records(sujet).forEach(r -> {
                    if (aggregateId.equals(r.key())) {
                        liste.add(r);
                    }
                });
            }
            return liste;
        }
    }
}
