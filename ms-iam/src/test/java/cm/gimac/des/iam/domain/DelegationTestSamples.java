package cm.gimac.des.iam.domain;

import java.util.UUID;

public class DelegationTestSamples {

    public static Delegation getDelegationSample1() {
        return new Delegation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Delegation getDelegationSample2() {
        return new Delegation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Delegation getDelegationRandomSampleGenerator() {
        return new Delegation()
            .id(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .requesterId(UUID.randomUUID())
            .approverId(UUID.randomUUID());
    }
}
