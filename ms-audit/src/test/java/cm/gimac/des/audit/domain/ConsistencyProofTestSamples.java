package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ConsistencyProofTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static ConsistencyProof getConsistencyProofSample1() {
        return new ConsistencyProof().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).firstSize(1L).secondSize(1L);
    }

    public static ConsistencyProof getConsistencyProofSample2() {
        return new ConsistencyProof().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).firstSize(2L).secondSize(2L);
    }

    public static ConsistencyProof getConsistencyProofRandomSampleGenerator() {
        return new ConsistencyProof().id(UUID.randomUUID()).firstSize(longCount.incrementAndGet()).secondSize(longCount.incrementAndGet());
    }
}
