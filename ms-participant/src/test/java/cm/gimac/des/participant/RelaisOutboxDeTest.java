package cm.gimac.des.participant;

import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxRelay;
import java.time.Duration;
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
 * Relais de l'outbox et lecture d'un sujet pour les tests d'intégration et Cucumber du lot 1.
 *
 * <p>La planification est désactivée dans les profils de test générés (AsyncConfiguration) : le
 * relais est déclenché explicitement, ce qui rend la vérification déterministe. Le premier envoi
 * vers un sujet créé à la volée peut échouer, comme en production où le relais réessaie toutes
 * les 500 ms : on relaie donc jusqu'à l'envoi effectif.
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

    public void relayer() {
        relais.relay();
    }

    public void relayerJusquaEnvoi(String aggregateId) throws InterruptedException {
        for (int essai = 0; essai < 20; essai++) {
            relais.relay();
            Integer envoyes = jdbc.queryForObject(
                "select count(*) from outbox_event where aggregate_id = ? and status = 'SENT'",
                Integer.class,
                aggregateId
            );
            if (envoyes != null && envoyes > 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Événement de " + aggregateId + " non relayé vers Kafka");
    }

    public Consumer<String, String> consommateur(String sujet) {
        var props = KafkaTestUtils.consumerProps(String.join(",", kafka.getBootstrapServers()), "it-" + UUID.randomUUID(), "true");
        Consumer<String, String> c = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer()).createConsumer();
        c.subscribe(List.of(sujet));
        return c;
    }

    /** Enregistrements du sujet dont la clé est l'agrégat attendu. */
    public List<ConsumerRecord<String, String>> lire(String sujet, String aggregateId) {
        try (Consumer<String, String> c = consommateur(sujet)) {
            var recus = KafkaTestUtils.getRecords(c, Duration.ofSeconds(20)).records(sujet);
            var liste = new java.util.ArrayList<ConsumerRecord<String, String>>();
            recus.forEach(r -> {
                if (aggregateId.equals(r.key())) {
                    liste.add(r);
                }
            });
            return liste;
        }
    }
}
