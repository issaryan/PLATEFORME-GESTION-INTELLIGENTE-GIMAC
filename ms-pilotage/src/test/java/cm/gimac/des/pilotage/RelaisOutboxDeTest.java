package cm.gimac.des.pilotage;

import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxRelay;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Relais de l'outbox et lecture d'un sujet pour les tests d'intégration de Mon activité. La
 * planification est désactivée dans les profils de test : le relais est déclenché explicitement,
 * jusqu'à l'envoi effectif (le premier envoi vers un sujet créé à la volée peut échouer).
 */
@Component
public class RelaisOutboxDeTest {

    private final OutboxRelay relais;
    private final JdbcTemplate jdbc;
    private final String courtier;

    public RelaisOutboxDeTest(OutboxRelay relais, JdbcTemplate jdbc, @Value("${spring.kafka.bootstrap-servers}") String courtier) {
        this.relais = relais;
        this.jdbc = jdbc;
        this.courtier = courtier;
    }

    public void relayerJusquaEnvoi(String aggregateId) throws InterruptedException {
        for (int essai = 0; essai < 20; essai++) {
            relais.relay();
            Integer envoyes = jdbc.queryForObject("select count(*) from outbox_event where aggregate_id = ? and status = 'SENT'", Integer.class, aggregateId);
            if (envoyes != null && envoyes > 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Événement de " + aggregateId + " non relayé vers Kafka");
    }

    /** Enregistrements du sujet dont la clé est l'agrégat attendu (lus depuis le début, 20 s au plus). */
    public List<ConsumerRecord<String, String>> lire(String sujet, String aggregateId) {
        Map<String, Object> props = Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            courtier,
            ConsumerConfig.GROUP_ID_CONFIG,
            "it-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest"
        );
        List<ConsumerRecord<String, String>> liste = new ArrayList<>();
        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer())) {
            c.subscribe(List.of(sujet));
            long fin = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (liste.isEmpty() && System.nanoTime() < fin) {
                c.poll(Duration.ofMillis(500)).records(sujet).forEach(r -> {
                    if (aggregateId.equals(r.key())) {
                        liste.add(r);
                    }
                });
            }
        }
        return liste;
    }
}
