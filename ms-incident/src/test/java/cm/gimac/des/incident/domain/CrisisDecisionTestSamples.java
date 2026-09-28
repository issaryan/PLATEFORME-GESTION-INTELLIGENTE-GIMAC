package cm.gimac.des.incident.domain;

import java.util.UUID;

public class CrisisDecisionTestSamples {

    public static CrisisDecision getCrisisDecisionSample1() {
        return new CrisisDecision()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .statement("statement1")
            .decidedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .taskRef("taskRef1");
    }

    public static CrisisDecision getCrisisDecisionSample2() {
        return new CrisisDecision()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .statement("statement2")
            .decidedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .taskRef("taskRef2");
    }

    public static CrisisDecision getCrisisDecisionRandomSampleGenerator() {
        return new CrisisDecision()
            .id(UUID.randomUUID())
            .statement(UUID.randomUUID().toString())
            .decidedBy(UUID.randomUUID())
            .taskRef(UUID.randomUUID().toString());
    }
}
