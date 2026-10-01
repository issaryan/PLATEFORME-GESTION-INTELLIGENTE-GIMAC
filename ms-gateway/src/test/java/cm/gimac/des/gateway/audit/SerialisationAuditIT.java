package cm.gimac.des.gateway.audit;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.IntegrationTest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Audit de la passerelle sur Kafka : le liant de Spring Cloud Stream impose par défaut des sérialiseurs
 * binaires au producteur de Spring Boot, que GatewayAuditEventPublisher (KafkaTemplate&lt;String, String&gt;)
 * ne peut pas employer. Défaut relevé sur la pile Compose réelle : aucun événement d'audit n'était publié.
 */
@IntegrationTest
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=localhost:9092")
class SerialisationAuditIT {

    @Autowired
    @Qualifier(ProducteurAuditConfiguration.MODELE_AUDIT)
    private KafkaTemplate<String, String> modeleAudit;

    @Test
    @DisplayName("Producteur de l'audit : clés et valeurs sérialisées en texte")
    void serialiseursTexte() {
        Object cle = modeleAudit.getProducerFactory().getConfigurationProperties().get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        Object valeur = modeleAudit.getProducerFactory().getConfigurationProperties().get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        assertThat(nom(cle)).isEqualTo(StringSerializer.class.getName());
        assertThat(nom(valeur)).isEqualTo(StringSerializer.class.getName());
    }

    private static String nom(Object o) {
        return o instanceof Class<?> c ? c.getName() : String.valueOf(o);
    }
}
