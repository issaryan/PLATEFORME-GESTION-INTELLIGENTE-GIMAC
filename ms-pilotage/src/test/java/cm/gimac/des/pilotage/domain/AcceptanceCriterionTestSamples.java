package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class AcceptanceCriterionTestSamples {

    public static AcceptanceCriterion getAcceptanceCriterionSample1() {
        return new AcceptanceCriterion()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .label("label1")
            .evidenceRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static AcceptanceCriterion getAcceptanceCriterionSample2() {
        return new AcceptanceCriterion()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .label("label2")
            .evidenceRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static AcceptanceCriterion getAcceptanceCriterionRandomSampleGenerator() {
        return new AcceptanceCriterion().id(UUID.randomUUID()).label(UUID.randomUUID().toString()).evidenceRef(UUID.randomUUID());
    }
}
