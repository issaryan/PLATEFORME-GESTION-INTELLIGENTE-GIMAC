package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EscalationTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Escalation getEscalationSample1() {
        return new Escalation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .level(1)
            .targetUserId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .targetContactId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1");
    }

    public static Escalation getEscalationSample2() {
        return new Escalation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .level(2)
            .targetUserId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .targetContactId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2");
    }

    public static Escalation getEscalationRandomSampleGenerator() {
        return new Escalation()
            .id(UUID.randomUUID())
            .level(intCount.incrementAndGet())
            .targetUserId(UUID.randomUUID())
            .targetContactId(UUID.randomUUID())
            .reason(UUID.randomUUID().toString());
    }
}
