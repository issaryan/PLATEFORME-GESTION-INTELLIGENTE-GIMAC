package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MessageMetadataTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static MessageMetadata getMessageMetadataSample1() {
        return new MessageMetadata()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .matrixEventId("matrixEventId1")
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .terminalId("terminalId1")
            .sessionId("sessionId1")
            .ratchetIndex(1L)
            .ciphertextSize(1)
            .attachmentRefs("attachmentRefs1");
    }

    public static MessageMetadata getMessageMetadataSample2() {
        return new MessageMetadata()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .matrixEventId("matrixEventId2")
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .terminalId("terminalId2")
            .sessionId("sessionId2")
            .ratchetIndex(2L)
            .ciphertextSize(2)
            .attachmentRefs("attachmentRefs2");
    }

    public static MessageMetadata getMessageMetadataRandomSampleGenerator() {
        return new MessageMetadata()
            .id(UUID.randomUUID())
            .matrixEventId(UUID.randomUUID().toString())
            .authorId(UUID.randomUUID())
            .terminalId(UUID.randomUUID().toString())
            .sessionId(UUID.randomUUID().toString())
            .ratchetIndex(longCount.incrementAndGet())
            .ciphertextSize(intCount.incrementAndGet())
            .attachmentRefs(UUID.randomUUID().toString());
    }
}
