package cm.gimac.des.participant.domain;

import java.util.UUID;

public class InteractionTestSamples {

    public static Interaction getInteractionSample1() {
        return new Interaction()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .summary("summary1")
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .notificationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .sourceEventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Interaction getInteractionSample2() {
        return new Interaction()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .summary("summary2")
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .notificationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .sourceEventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Interaction getInteractionRandomSampleGenerator() {
        return new Interaction()
            .id(UUID.randomUUID())
            .summary(UUID.randomUUID().toString())
            .authorId(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .notificationId(UUID.randomUUID())
            .sourceEventId(UUID.randomUUID());
    }
}
