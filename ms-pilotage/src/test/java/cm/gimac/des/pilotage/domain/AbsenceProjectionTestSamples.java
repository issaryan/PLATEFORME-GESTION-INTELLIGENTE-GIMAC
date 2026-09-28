package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class AbsenceProjectionTestSamples {

    public static AbsenceProjection getAbsenceProjectionSample1() {
        return new AbsenceProjection()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .availabilityId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .collaboratorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static AbsenceProjection getAbsenceProjectionSample2() {
        return new AbsenceProjection()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .availabilityId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .collaboratorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static AbsenceProjection getAbsenceProjectionRandomSampleGenerator() {
        return new AbsenceProjection().id(UUID.randomUUID()).availabilityId(UUID.randomUUID()).collaboratorId(UUID.randomUUID());
    }
}
