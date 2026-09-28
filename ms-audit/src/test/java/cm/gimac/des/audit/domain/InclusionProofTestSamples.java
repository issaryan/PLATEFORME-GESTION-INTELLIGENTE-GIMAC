package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InclusionProofTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static InclusionProof getInclusionProofSample1() {
        return new InclusionProof()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .eventIndex(1L)
            .treeSize(1L)
            .rootHash("rootHash1");
    }

    public static InclusionProof getInclusionProofSample2() {
        return new InclusionProof()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .eventIndex(2L)
            .treeSize(2L)
            .rootHash("rootHash2");
    }

    public static InclusionProof getInclusionProofRandomSampleGenerator() {
        return new InclusionProof()
            .id(UUID.randomUUID())
            .eventIndex(longCount.incrementAndGet())
            .treeSize(longCount.incrementAndGet())
            .rootHash(UUID.randomUUID().toString());
    }
}
