package cm.gimac.des.notification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Notification getNotificationSample1() {
        return new Notification()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .notificationType("notificationType1")
            .subject("subject1")
            .objectType("objectType1")
            .objectId("objectId1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .sourceEventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .sourceService("sourceService1")
            .correlationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .groupKey("groupKey1")
            .escalationLevel(1)
            .acknowledgedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .occurrenceCount(1);
    }

    public static Notification getNotificationSample2() {
        return new Notification()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .notificationType("notificationType2")
            .subject("subject2")
            .objectType("objectType2")
            .objectId("objectId2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .sourceEventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .sourceService("sourceService2")
            .correlationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .groupKey("groupKey2")
            .escalationLevel(2)
            .acknowledgedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .occurrenceCount(2);
    }

    public static Notification getNotificationRandomSampleGenerator() {
        return new Notification()
            .id(UUID.randomUUID())
            .notificationType(UUID.randomUUID().toString())
            .subject(UUID.randomUUID().toString())
            .objectType(UUID.randomUUID().toString())
            .objectId(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .sourceEventId(UUID.randomUUID())
            .sourceService(UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID())
            .groupKey(UUID.randomUUID().toString())
            .escalationLevel(intCount.incrementAndGet())
            .acknowledgedBy(UUID.randomUUID())
            .occurrenceCount(intCount.incrementAndGet());
    }
}
