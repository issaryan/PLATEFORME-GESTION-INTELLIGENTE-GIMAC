package cm.gimac.des.document.domain;

import java.util.UUID;

public class PurgeRequestTestSamples {

    public static PurgeRequest getPurgeRequestSample1() {
        return new PurgeRequest()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .rejectionReason("rejectionReason1");
    }

    public static PurgeRequest getPurgeRequestSample2() {
        return new PurgeRequest()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .rejectionReason("rejectionReason2");
    }

    public static PurgeRequest getPurgeRequestRandomSampleGenerator() {
        return new PurgeRequest()
            .id(UUID.randomUUID())
            .requesterId(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .approverId(UUID.randomUUID())
            .rejectionReason(UUID.randomUUID().toString());
    }
}
