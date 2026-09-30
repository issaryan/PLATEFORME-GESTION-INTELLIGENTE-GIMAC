package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Room getRoomSample1() {
        return new Room()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .attachmentReference("attachmentReference1")
            .attachmentService("attachmentService1")
            .ticketReference("ticketReference1")
            .matrixRoomId("matrixRoomId1")
            .encryption("encryption1")
            .groupSessionEpoch(1);
    }

    public static Room getRoomSample2() {
        return new Room()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .attachmentReference("attachmentReference2")
            .attachmentService("attachmentService2")
            .ticketReference("ticketReference2")
            .matrixRoomId("matrixRoomId2")
            .encryption("encryption2")
            .groupSessionEpoch(2);
    }

    public static Room getRoomRandomSampleGenerator() {
        return new Room()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .attachmentReference(UUID.randomUUID().toString())
            .attachmentService(UUID.randomUUID().toString())
            .ticketReference(UUID.randomUUID().toString())
            .matrixRoomId(UUID.randomUUID().toString())
            .encryption(UUID.randomUUID().toString())
            .groupSessionEpoch(intCount.incrementAndGet());
    }
}
