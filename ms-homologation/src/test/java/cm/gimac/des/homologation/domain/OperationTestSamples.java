package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class OperationTestSamples {

    public static Operation getOperationSample1() {
        return new Operation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .operationCode("operationCode1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDocumentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDigest("evidenceDigest1")
            .evidenceTraceRef("evidenceTraceRef1")
            .timestampImprint("timestampImprint1")
            .validatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .stateReason("stateReason1");
    }

    public static Operation getOperationSample2() {
        return new Operation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .operationCode("operationCode2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDocumentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDigest("evidenceDigest2")
            .evidenceTraceRef("evidenceTraceRef2")
            .timestampImprint("timestampImprint2")
            .validatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .stateReason("stateReason2");
    }

    public static Operation getOperationRandomSampleGenerator() {
        return new Operation()
            .id(UUID.randomUUID())
            .operationCode(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .evidenceDocumentId(UUID.randomUUID())
            .evidenceDigest(UUID.randomUUID().toString())
            .evidenceTraceRef(UUID.randomUUID().toString())
            .timestampImprint(UUID.randomUUID().toString())
            .validatorId(UUID.randomUUID())
            .stateReason(UUID.randomUUID().toString());
    }
}
