package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class OperatorUnavailabilityTestSamples {

    public static OperatorUnavailability getOperatorUnavailabilitySample1() {
        return new OperatorUnavailability()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .availabilityId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .operatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static OperatorUnavailability getOperatorUnavailabilitySample2() {
        return new OperatorUnavailability()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .availabilityId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .operatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static OperatorUnavailability getOperatorUnavailabilityRandomSampleGenerator() {
        return new OperatorUnavailability().id(UUID.randomUUID()).availabilityId(UUID.randomUUID()).operatorId(UUID.randomUUID());
    }
}
