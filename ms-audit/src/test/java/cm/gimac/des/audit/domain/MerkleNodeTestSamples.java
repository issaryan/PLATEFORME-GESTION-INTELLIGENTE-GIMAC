package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MerkleNodeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static MerkleNode getMerkleNodeSample1() {
        return new MerkleNode().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).level(1).position(1L).hash("hash1");
    }

    public static MerkleNode getMerkleNodeSample2() {
        return new MerkleNode().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).level(2).position(2L).hash("hash2");
    }

    public static MerkleNode getMerkleNodeRandomSampleGenerator() {
        return new MerkleNode()
            .id(UUID.randomUUID())
            .level(intCount.incrementAndGet())
            .position(longCount.incrementAndGet())
            .hash(UUID.randomUUID().toString());
    }
}
