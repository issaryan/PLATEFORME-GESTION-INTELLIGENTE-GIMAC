package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ReferenceOperationTestSamples {

    public static ReferenceOperation getReferenceOperationSample1() {
        return new ReferenceOperation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .branch("branch1")
            .expectedEvidence("expectedEvidence1")
            .serviceCode("serviceCode1")
            .scenarioRef("scenarioRef1");
    }

    public static ReferenceOperation getReferenceOperationSample2() {
        return new ReferenceOperation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .branch("branch2")
            .expectedEvidence("expectedEvidence2")
            .serviceCode("serviceCode2")
            .scenarioRef("scenarioRef2");
    }

    public static ReferenceOperation getReferenceOperationRandomSampleGenerator() {
        return new ReferenceOperation()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .branch(UUID.randomUUID().toString())
            .expectedEvidence(UUID.randomUUID().toString())
            .serviceCode(UUID.randomUUID().toString())
            .scenarioRef(UUID.randomUUID().toString());
    }
}
