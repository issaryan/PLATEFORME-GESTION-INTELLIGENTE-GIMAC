package cm.gimac.des.certification.infrastructure.messaging.outbox;

import cm.gimac.des.certification.infrastructure.security.CorrelationContext;
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
        return enregistrer(aggregateType, aggregateId, eventType, payload, ctx.institutionId().orElse(null));
    }

    private UUID enregistrer(String aggregateType, String aggregateId, String eventType, Object payload, String institutionId) {
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
     * Variante à institution explicite : un acte du DES concerne l'institution de la liaison, que le jeton
     * de l'agent ne porte pas ; l'extension institutionid sert le canal poussé du portail (parcours-backend §3).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(String aggregateType, UUID aggregateId, String eventType, Object payload, String institutionId) {
        String institution = institutionId != null && !institutionId.isBlank() ? institutionId : ctx.institutionId().orElse(null);
        return enregistrer(aggregateType, aggregateId.toString(), eventType, payload, institution);
    }
}
