package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomArchiveTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static RoomArchive getRoomArchiveSample1() {
        return new RoomArchive()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .crisisCellReference("crisisCellReference1")
            .ticketReference("ticketReference1")
            .messageCount(1)
            .undecryptableCount(1)
            .maskedSequences(1)
            .chainHead("chainHead1")
            .contentDigest("contentDigest1")
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .retentionYears(1);
    }

    public static RoomArchive getRoomArchiveSample2() {
        return new RoomArchive()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .crisisCellReference("crisisCellReference2")
            .ticketReference("ticketReference2")
            .messageCount(2)
            .undecryptableCount(2)
            .maskedSequences(2)
            .chainHead("chainHead2")
            .contentDigest("contentDigest2")
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .retentionYears(2);
    }

    public static RoomArchive getRoomArchiveRandomSampleGenerator() {
        return new RoomArchive()
            .id(UUID.randomUUID())
            .crisisCellReference(UUID.randomUUID().toString())
            .ticketReference(UUID.randomUUID().toString())
            .messageCount(intCount.incrementAndGet())
            .undecryptableCount(intCount.incrementAndGet())
            .maskedSequences(intCount.incrementAndGet())
            .chainHead(UUID.randomUUID().toString())
            .contentDigest(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID())
            .retentionYears(intCount.incrementAndGet());
    }
}
