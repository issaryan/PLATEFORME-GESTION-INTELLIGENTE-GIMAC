package cm.gimac.des.gateway.infrastructure.sandbox;

import cm.gimac.des.gateway.domain.sandbox.port.ProjectionPerimetres;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

/**
 * Consommateur de sbx-espace.scope-changed.v1 : groupe partagé par les instances de la passerelle (la projection est en
 * base), lecture depuis le début, validation enregistrement par enregistrement après application. Idempotent : une
 * redélivrance ou un instantané périmé (version inférieure ou égale) est sans effet ; une enveloppe non conforme est
 * écartée et journalisée.
 */
public class ConsommateurPerimetresKafka implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(ConsommateurPerimetresKafka.class);
    public static final String GROUPE = "gateway-sbx-perimetres";
    static final Duration DELAI_APPLICATION = Duration.ofSeconds(10);

    private final ProjectionPerimetres projection;
    private final LecturePerimetre lecture = new LecturePerimetre();
    private final KafkaMessageListenerContainer<String, String> conteneur;

    public ConsommateurPerimetresKafka(Map<String, Object> proprietes, String sujet, ProjectionPerimetres projection) {
        this.projection = projection;
        Map<String, Object> config = new HashMap<>(proprietes);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUPE);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ContainerProperties cp = new ContainerProperties(sujet);
        cp.setGroupId(GROUPE);
        cp.setAckMode(ContainerProperties.AckMode.RECORD);
        cp.setMissingTopicsFatal(false);
        cp.setMessageListener((MessageListener<String, String>) this::recevoir);
        this.conteneur = new KafkaMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(config), cp);
        this.conteneur.setBeanName(GROUPE);
    }

    /** Enregistrement reçu : instantané lu puis appliqué ; une enveloppe non conforme est écartée. */
    void recevoir(ConsumerRecord<String, String> enregistrement) {
        var perimetre = lecture.lire(enregistrement.value());
        if (perimetre.isEmpty()) {
            LOG.warn("Instantané de périmètre écarté (enveloppe non conforme) : {}#{}", enregistrement.topic(), enregistrement.offset());
            return;
        }
        Boolean applique = projection.appliquer(perimetre.orElseThrow()).block(DELAI_APPLICATION);
        LOG.debug("Périmètre du compte {} en version {} : {}", perimetre.orElseThrow().compte(), perimetre.orElseThrow().version(), Boolean.TRUE.equals(applique) ? "appliqué" : "sans effet");
    }

    @Override
    public void start() {
        conteneur.start();
    }

    @Override
    public void stop() {
        conteneur.stop();
    }

    @Override
    public boolean isRunning() {
        return conteneur.isRunning();
    }
}
