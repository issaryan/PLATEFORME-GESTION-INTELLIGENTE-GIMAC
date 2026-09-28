package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Task getTaskSample1() {
        return new Task()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .title("title1")
            .assigneeId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .originReference("originReference1")
            .originService("originService1")
            .reminderLevel(1);
    }

    public static Task getTaskSample2() {
        return new Task()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .title("title2")
            .assigneeId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .originReference("originReference2")
            .originService("originService2")
            .reminderLevel(2);
    }

    public static Task getTaskRandomSampleGenerator() {
        return new Task()
            .id(UUID.randomUUID())
            .title(UUID.randomUUID().toString())
            .assigneeId(UUID.randomUUID())
            .originReference(UUID.randomUUID().toString())
            .originService(UUID.randomUUID().toString())
            .reminderLevel(intCount.incrementAndGet());
    }
}
