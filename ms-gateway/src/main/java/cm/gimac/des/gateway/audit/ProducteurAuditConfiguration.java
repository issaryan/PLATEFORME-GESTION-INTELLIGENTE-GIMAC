package cm.gimac.des.gateway.audit;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Producteur Kafka propre à l'audit de la passerelle (sujet gateway.security.events). Le liant Kafka de
 * Spring Cloud Stream impose des sérialiseurs binaires au producteur de Spring Boot, au-dessus de la
 * configuration ; l'audit, publié en JSON texte, a donc son producteur aux sérialiseurs explicites.
 * Défaut relevé sur la pile Compose réelle : aucun événement d'audit (mutations, connexions D-65) n'était publié.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class ProducteurAuditConfiguration {

    public static final String MODELE_AUDIT = "kafkaTemplateAudit";

    @Bean(MODELE_AUDIT)
    public KafkaTemplate<String, String> kafkaTemplateAudit(KafkaProperties proprietes) {
        Map<String, Object> config = proprietes.buildProducerProperties();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }
}
