package cm.gimac.des.pilotage.config;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.IntegrationTest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

/** Sépare les messages byte[] du binder Cloud Stream des enveloppes CloudEvents String de l'outbox. */
@IntegrationTest
class SerialisationKafkaIT {

    @Autowired
    private Environment environment;

    @Autowired
    private KafkaTemplate<String, String> outbox;

    @Test
    @DisplayName("Le binder sérialise les byte[] et l'outbox conserve ses CloudEvents en texte")
    void serialiseursBinderEtOutbox() {
        String prefixe = "spring.cloud.stream.kafka.binder.producer-properties.";
        assertThat(environment.getProperty(prefixe + ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG))
            .isEqualTo(ByteArraySerializer.class.getName());
        assertThat(environment.getProperty(prefixe + ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
            .isEqualTo(ByteArraySerializer.class.getName());
        assertThat(outbox.getProducerFactory().getConfigurationProperties())
            .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    }
}
