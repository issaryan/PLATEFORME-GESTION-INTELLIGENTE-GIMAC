package cm.gimac.des.collaboration.infrastructure.messaging;

import cm.gimac.des.collaboration.application.port.out.SignalEphemerePort;
import cm.gimac.des.collaboration.domain.event.SaisiePayload;
import cm.gimac.des.collaboration.infrastructure.security.CorrelationContext;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Signaux éphémères de la messagerie (D-72) : enveloppe CloudEvents publiée sans attendre, hors outbox,
 * sur le sujet de l'agrégat (collaboration.message) que la passerelle relaie au canal poussé. Un échec
 * est seulement journalisé : le signal suivant arrive dans deux secondes.
 */
@Component
public class KafkaSignalEphemere implements SignalEphemerePort {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaSignalEphemere.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final TopicResolver topics;
    private final Clock clock;
    private final String source;

    public KafkaSignalEphemere(
        KafkaTemplate<String, String> kafka,
        ObjectMapper mapper,
        TopicResolver topics,
        Clock clock,
        @Value("${spring.application.name}") String app
    ) {
        this.kafka = kafka;
        this.mapper = mapper;
        this.topics = topics;
        this.clock = clock;
        this.source = "/gimac/des/" + app;
    }

    @Override
    public void saisie(UUID conversationId, String auteurId, List<String> destinataires) {
        var le = clock.instant();
        var enveloppe = new CloudEventEnvelope(
            "1.0",
            UUID.randomUUID().toString(),
            source,
            SaisiePayload.TYPE,
            conversationId.toString(),
            le.toString(),
            "application/json",
            CorrelationContext.current(),
            null,
            mapper.valueToTree(new SaisiePayload(conversationId, auteurId, List.copyOf(destinataires), le))
        );
        try {
            kafka
                .send(topics.topicFor(SaisiePayload.TYPE), conversationId.toString(), mapper.writeValueAsString(enveloppe))
                .whenComplete((r, e) -> {
                    if (e != null) {
                        LOG.debug("Signal de saisie non publié pour {} : {}", conversationId, e.toString());
                    }
                });
        } catch (RuntimeException e) {
            LOG.debug("Signal de saisie non publié pour {} : {}", conversationId, e.toString());
        }
    }
}
