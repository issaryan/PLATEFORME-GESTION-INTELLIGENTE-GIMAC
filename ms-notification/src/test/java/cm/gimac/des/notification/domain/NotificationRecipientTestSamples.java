package cm.gimac.des.notification.domain;

import java.util.UUID;

public class NotificationRecipientTestSamples {

    public static NotificationRecipient getNotificationRecipientSample1() {
        return new NotificationRecipient()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .channels("channels1");
    }

    public static NotificationRecipient getNotificationRecipientSample2() {
        return new NotificationRecipient()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .channels("channels2");
    }

    public static NotificationRecipient getNotificationRecipientRandomSampleGenerator() {
        return new NotificationRecipient()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .channels(UUID.randomUUID().toString());
    }
}
