package cm.gimac.des.homologation.infrastructure.messaging.outbox;

import cm.gimac.des.homologation.infrastructure.security.CorrelationContext;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Écrit l'événement dans l'outbox, dans la transaction de l'agrégat (PI §0.4). */
@Component
public class OutboxPublisher {

    private final OutboxEventRepository repo;
    private final ObjectMapper mapper;
    private final CorrelationContext ctx;

    public OutboxPublisher(OutboxEventRepository repo, ObjectMapper mapper, CorrelationContext ctx) {
        this.repo = repo;
        this.mapper = mapper;
        this.ctx = ctx;
    }

    /** À appeler dans la transaction de l'agrégat : l'événement part avec elle ou pas du tout. */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        return ecrire(aggregateType, aggregateId, eventType, payload, ctx.institutionId().orElse(null));
    }

    private UUID ecrire(String aggregateType, String aggregateId, String eventType, Object payload, String institutionId) {
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        try {
            e.setPayload(mapper.writeValueAsString(payload));
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("charge utile non sérialisable", ex);
        }
        e.setCorrelationId(ctx.correlationId());
        e.setInstitutionId(institutionId);
        return repo.save(e).getId();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        return publish(aggregateType, aggregateId.toString(), eventType, payload);
    }

    /**
     * Variante qui porte l'institution de l'agrégat (extension CloudEvents institutionid) quand l'acteur
     * est un collaborateur du DES, dont le jeton n'en porte pas : le canal poussé du portail s'en sert.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, UUID aggregateId, String eventType, Object payload, String institutionId) {
        return ecrire(aggregateType, aggregateId.toString(), eventType, payload, institutionId);
    }
}
