package cm.gimac.des.notification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationSubscriptionTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static NotificationSubscription getNotificationSubscriptionSample1() {
        return new NotificationSubscription()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .notificationType("notificationType1")
            .ruleValue("ruleValue1")
            .ruleVersion(1)
            .updatedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static NotificationSubscription getNotificationSubscriptionSample2() {
        return new NotificationSubscription()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .notificationType("notificationType2")
            .ruleValue("ruleValue2")
            .ruleVersion(2)
            .updatedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static NotificationSubscription getNotificationSubscriptionRandomSampleGenerator() {
        return new NotificationSubscription()
            .id(UUID.randomUUID())
            .notificationType(UUID.randomUUID().toString())
            .ruleValue(UUID.randomUUID().toString())
            .ruleVersion(intCount.incrementAndGet())
            .updatedBy(UUID.randomUUID());
    }
}
