package cm.gimac.des.participant.domain;

import java.util.UUID;

public class DeadlineTestSamples {

    public static Deadline getDeadlineSample1() {
        return new Deadline()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .triggeredAction("triggeredAction1")
            .notificationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Deadline getDeadlineSample2() {
        return new Deadline()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .triggeredAction("triggeredAction2")
            .notificationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Deadline getDeadlineRandomSampleGenerator() {
        return new Deadline().id(UUID.randomUUID()).triggeredAction(UUID.randomUUID().toString()).notificationId(UUID.randomUUID());
    }
}
