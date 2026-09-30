package cm.gimac.des.gateway.infrastructure.live;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import cm.gimac.des.gateway.domain.live.port.SourceEvenements;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Source Kafka du canal poussé : un consommateur par instance de la passerelle, dans son propre
 * groupe (« gateway-live-&lt;aléa&gt; »), positionné à la fin des sujets. C'est une diffusion de
 * notifications, pas un traitement : aucun décalage n'est validé, un redémarrage ne rejoue rien.
 * Courtier injoignable : le conteneur réessaie en arrière-plan sans bloquer le démarrage.
 */
public class KafkaSourceEvenements implements SourceEvenements, SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceEvenements.class);

    public static final String PREFIXE_GROUPE = "gateway-live-";

    private final Sinks.Many<EvenementLive> diffusion = Sinks.many().multicast().directBestEffort();
    private final LectureCloudEvent lecture;
    private final KafkaMessageListenerContainer<String, String> conteneur;
    private final String groupe;

    /**
     * @param proprietes propriétés du consommateur (courtiers, sécurité…) ; groupe, positionnement,
     *        validation et désérialiseurs sont imposés ici
     */
    public KafkaSourceEvenements(Map<String, Object> proprietes, List<String> sujets, LectureCloudEvent lecture) {
        this.lecture = lecture;
        this.groupe = PREFIXE_GROUPE + UUID.randomUUID();
        Map<String, Object> config = new HashMap<>(proprietes);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupe);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        ContainerProperties cp = new ContainerProperties(sujets.toArray(String[]::new));
        cp.setGroupId(groupe);
        // Aucune validation : le groupe est propre à l'instance et éphémère.
        cp.setAckMode(ContainerProperties.AckMode.MANUAL);
        cp.setMissingTopicsFatal(false);
        cp.setMessageListener((AcknowledgingMessageListener<String, String>) (enregistrement, ack) -> recevoir(enregistrement));
        this.conteneur = new KafkaMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(config), cp);
        this.conteneur.setBeanName("gateway-live");
    }

    /** Enregistrement reçu : enveloppe lue puis diffusée ; jamais d'exception vers le conteneur. */
    void recevoir(ConsumerRecord<String, String> enregistrement) {
        try {
            lecture.lire(enregistrement.topic(), enregistrement.value()).ifPresent(diffusion::tryEmitNext);
        } catch (RuntimeException e) {
            LOG.warn("Événement ignoré sur {} : {}", enregistrement.topic(), e.getMessage());
        }
    }

    @Override
    public Flux<EvenementLive> evenements() {
        return diffusion.asFlux();
    }

    public String groupe() {
        return groupe;
    }

    /** Partitions attribuées au consommateur (0 tant que le courtier n'a pas répondu). */
    public int partitionsAttribuees() {
        var partitions = conteneur.getAssignedPartitions();
        return partitions == null ? 0 : partitions.size();
    }

    @Override
    public void start() {
        LOG.info("Canal poussé : consommation de {} (groupe {})", List.of(conteneur.getContainerProperties().getTopics()), groupe);
        conteneur.start();
    }

    @Override
    public void stop() {
        conteneur.stop();
        diffusion.tryEmitComplete();
    }

    @Override
    public boolean isRunning() {
        return conteneur.isRunning();
    }
}
