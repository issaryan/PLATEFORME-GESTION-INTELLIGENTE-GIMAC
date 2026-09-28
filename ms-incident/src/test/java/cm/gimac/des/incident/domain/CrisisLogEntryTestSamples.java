package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class CrisisLogEntryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static CrisisLogEntry getCrisisLogEntrySample1() {
        return new CrisisLogEntry()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .chainIndex(1L)
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .text("text1")
            .reference("reference1")
            .previousHash("previousHash1")
            .hash("hash1");
    }

    public static CrisisLogEntry getCrisisLogEntrySample2() {
        return new CrisisLogEntry()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .chainIndex(2L)
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .text("text2")
            .reference("reference2")
            .previousHash("previousHash2")
            .hash("hash2");
    }

    public static CrisisLogEntry getCrisisLogEntryRandomSampleGenerator() {
        return new CrisisLogEntry()
            .id(UUID.randomUUID())
            .chainIndex(longCount.incrementAndGet())
            .authorId(UUID.randomUUID())
            .text(UUID.randomUUID().toString())
            .reference(UUID.randomUUID().toString())
            .previousHash(UUID.randomUUID().toString())
            .hash(UUID.randomUUID().toString());
    }
}
