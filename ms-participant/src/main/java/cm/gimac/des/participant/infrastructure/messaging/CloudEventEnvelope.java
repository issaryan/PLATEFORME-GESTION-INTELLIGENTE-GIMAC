package cm.gimac.des.participant.infrastructure.messaging;

import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxEvent;
import tools.jackson.databind.JsonNode;

/** Enveloppe CloudEvents 1.0 en mode structuré (PI §0.4). */
public record CloudEventEnvelope(
    String specversion,
    String id,
    String source,
    String type,
    String subject,
    String time,
    String datacontenttype,
    String correlationid,
    String institutionid,
    JsonNode data
) {
    public static CloudEventEnvelope of(OutboxEvent e, String source) {
        return new CloudEventEnvelope(
            "1.0",
            e.getId().toString(),
            source,
            e.getEventType(),
            e.getAggregateId(),
            e.getCreatedAt().toString(),
            "application/json",
            e.getCorrelationId(),
            e.getInstitutionId(),
            Json.tree(e.getPayload())
        );
    }
}
