package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TransitionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Transition getTransitionSample1() {
        return new Transition()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .chainIndex(1L)
            .actorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .justification("justification1")
            .previousHash("previousHash1")
            .hash("hash1");
    }

    public static Transition getTransitionSample2() {
        return new Transition()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .chainIndex(2L)
            .actorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .justification("justification2")
            .previousHash("previousHash2")
            .hash("hash2");
    }

    public static Transition getTransitionRandomSampleGenerator() {
        return new Transition()
            .id(UUID.randomUUID())
            .chainIndex(longCount.incrementAndGet())
            .actorId(UUID.randomUUID())
            .justification(UUID.randomUUID().toString())
            .previousHash(UUID.randomUUID().toString())
            .hash(UUID.randomUUID().toString());
    }
}
