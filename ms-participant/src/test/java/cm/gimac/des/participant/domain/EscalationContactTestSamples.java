package cm.gimac.des.participant.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EscalationContactTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static EscalationContact getEscalationContactSample1() {
        return new EscalationContact().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).severity(1).escalationRank(1);
    }

    public static EscalationContact getEscalationContactSample2() {
        return new EscalationContact().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).severity(2).escalationRank(2);
    }

    public static EscalationContact getEscalationContactRandomSampleGenerator() {
        return new EscalationContact()
            .id(UUID.randomUUID())
            .severity(intCount.incrementAndGet())
            .escalationRank(intCount.incrementAndGet());
    }
}
