package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DeduplicationEntryTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static DeduplicationEntry getDeduplicationEntrySample1() {
        return new DeduplicationEntry()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .eventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .auditIndex(1L);
    }

    public static DeduplicationEntry getDeduplicationEntrySample2() {
        return new DeduplicationEntry()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .eventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .auditIndex(2L);
    }

    public static DeduplicationEntry getDeduplicationEntryRandomSampleGenerator() {
        return new DeduplicationEntry().id(UUID.randomUUID()).eventId(UUID.randomUUID()).auditIndex(longCount.incrementAndGet());
    }
}
