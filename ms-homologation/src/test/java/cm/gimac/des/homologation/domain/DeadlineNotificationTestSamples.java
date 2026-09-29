package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class DeadlineNotificationTestSamples {

    public static DeadlineNotification getDeadlineNotificationSample1() {
        return new DeadlineNotification()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .operationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .seuil("seuil1");
    }

    public static DeadlineNotification getDeadlineNotificationSample2() {
        return new DeadlineNotification()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .operationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .seuil("seuil2");
    }

    public static DeadlineNotification getDeadlineNotificationRandomSampleGenerator() {
        return new DeadlineNotification().id(UUID.randomUUID()).operationId(UUID.randomUUID()).seuil(UUID.randomUUID().toString());
    }
}
