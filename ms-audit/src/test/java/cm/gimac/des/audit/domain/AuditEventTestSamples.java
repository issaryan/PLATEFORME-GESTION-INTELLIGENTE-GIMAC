package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AuditEventTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static AuditEvent getAuditEventSample1() {
        return new AuditEvent()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .logIndex(1L)
            .eventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .actorId("actorId1")
            .actionType("actionType1")
            .domain("domain1")
            .subject("subject1")
            .correlationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .leafHash("leafHash1");
    }

    public static AuditEvent getAuditEventSample2() {
        return new AuditEvent()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .logIndex(2L)
            .eventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .actorId("actorId2")
            .actionType("actionType2")
            .domain("domain2")
            .subject("subject2")
            .correlationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .leafHash("leafHash2");
    }

    public static AuditEvent getAuditEventRandomSampleGenerator() {
        return new AuditEvent()
            .id(UUID.randomUUID())
            .logIndex(longCount.incrementAndGet())
            .eventId(UUID.randomUUID())
            .actorId(UUID.randomUUID().toString())
            .actionType(UUID.randomUUID().toString())
            .domain(UUID.randomUUID().toString())
            .subject(UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID())
            .leafHash(UUID.randomUUID().toString());
    }
}
