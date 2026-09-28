package cm.gimac.des.rapprochement.domain;

import java.util.UUID;

public class SuspensTransitionTestSamples {

    public static SuspensTransition getSuspensTransitionSample1() {
        return new SuspensTransition()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .actorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .justification("justification1");
    }

    public static SuspensTransition getSuspensTransitionSample2() {
        return new SuspensTransition()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .actorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .justification("justification2");
    }

    public static SuspensTransition getSuspensTransitionRandomSampleGenerator() {
        return new SuspensTransition().id(UUID.randomUUID()).actorId(UUID.randomUUID()).justification(UUID.randomUUID().toString());
    }
}
