package cm.gimac.des.notification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AcknowledgementPolicyTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AcknowledgementPolicy getAcknowledgementPolicySample1() {
        return new AcknowledgementPolicy()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .notificationType("notificationType1")
            .maxEscalationLevel(1);
    }

    public static AcknowledgementPolicy getAcknowledgementPolicySample2() {
        return new AcknowledgementPolicy()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .notificationType("notificationType2")
            .maxEscalationLevel(2);
    }

    public static AcknowledgementPolicy getAcknowledgementPolicyRandomSampleGenerator() {
        return new AcknowledgementPolicy()
            .id(UUID.randomUUID())
            .notificationType(UUID.randomUUID().toString())
            .maxEscalationLevel(intCount.incrementAndGet());
    }
}
