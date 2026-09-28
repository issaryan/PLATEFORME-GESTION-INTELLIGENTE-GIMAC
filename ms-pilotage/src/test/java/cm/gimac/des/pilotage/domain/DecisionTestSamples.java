package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class DecisionTestSamples {

    public static Decision getDecisionSample1() {
        return new Decision()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .text("text1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .returnReason("returnReason1");
    }

    public static Decision getDecisionSample2() {
        return new Decision()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .text("text2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .returnReason("returnReason2");
    }

    public static Decision getDecisionRandomSampleGenerator() {
        return new Decision()
            .id(UUID.randomUUID())
            .text(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .returnReason(UUID.randomUUID().toString());
    }
}
