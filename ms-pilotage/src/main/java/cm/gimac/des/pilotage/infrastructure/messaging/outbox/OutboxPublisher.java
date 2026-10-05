package cm.gimac.des.pilotage.infrastructure.messaging.outbox;

import cm.gimac.des.pilotage.infrastructure.security.CorrelationContext;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        OutboxEvent e = new OutboxEvent();
        e.setAggregateType(aggregateType);
        e.setAggregateId(aggregateId);
        e.setEventType(eventType);
        try {
            e.setPayload(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("charge utile non sérialisable", ex);
        }
        e.setCorrelationId(ctx.correlationId());
        e.setInstitutionId(ctx.institutionId().orElse(null));
        return repo.save(e).getId();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        return publish(aggregateType, aggregateId.toString(), eventType, payload);
    }
}
