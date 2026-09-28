package cm.gimac.des.rapprochement.domain;

import java.util.UUID;

public class RegularizationAttestationTestSamples {

    public static RegularizationAttestation getRegularizationAttestationSample1() {
        return new RegularizationAttestation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentDigest("documentDigest1")
            .depositorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static RegularizationAttestation getRegularizationAttestationSample2() {
        return new RegularizationAttestation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentDigest("documentDigest2")
            .depositorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static RegularizationAttestation getRegularizationAttestationRandomSampleGenerator() {
        return new RegularizationAttestation()
            .id(UUID.randomUUID())
            .documentRef(UUID.randomUUID())
            .documentDigest(UUID.randomUUID().toString())
            .depositorId(UUID.randomUUID())
            .validatorId(UUID.randomUUID());
    }
}
