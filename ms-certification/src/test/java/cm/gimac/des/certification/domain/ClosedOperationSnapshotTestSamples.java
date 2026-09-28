package cm.gimac.des.certification.domain;

import java.util.UUID;

public class ClosedOperationSnapshotTestSamples {

    public static ClosedOperationSnapshot getClosedOperationSnapshotSample1() {
        return new ClosedOperationSnapshot()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .operationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .operationCode("operationCode1")
            .referenceCode("referenceCode1")
            .serviceCode("serviceCode1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDocumentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDigest("evidenceDigest1")
            .sourceEventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ClosedOperationSnapshot getClosedOperationSnapshotSample2() {
        return new ClosedOperationSnapshot()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .operationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .operationCode("operationCode2")
            .referenceCode("referenceCode2")
            .serviceCode("serviceCode2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDocumentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDigest("evidenceDigest2")
            .sourceEventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ClosedOperationSnapshot getClosedOperationSnapshotRandomSampleGenerator() {
        return new ClosedOperationSnapshot()
            .id(UUID.randomUUID())
            .liaisonId(UUID.randomUUID())
            .operationId(UUID.randomUUID())
            .operationCode(UUID.randomUUID().toString())
            .referenceCode(UUID.randomUUID().toString())
            .serviceCode(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .validatorId(UUID.randomUUID())
            .evidenceDocumentId(UUID.randomUUID())
            .evidenceDigest(UUID.randomUUID().toString())
            .sourceEventId(UUID.randomUUID());
    }
}
