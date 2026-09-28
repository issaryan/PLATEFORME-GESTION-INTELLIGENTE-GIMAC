package cm.gimac.des.iam.domain;

import java.util.UUID;

public class AvailabilityTestSamples {

    public static Availability getAvailabilitySample1() {
        return new Availability()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .justificationDocumentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Availability getAvailabilitySample2() {
        return new Availability()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .justificationDocumentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Availability getAvailabilityRandomSampleGenerator() {
        return new Availability()
            .id(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .justificationDocumentId(UUID.randomUUID())
            .validatorId(UUID.randomUUID());
    }
}
