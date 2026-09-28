package cm.gimac.des.incident.domain;

import java.util.UUID;

public class AlertObservationTestSamples {

    public static AlertObservation getAlertObservationSample1() {
        return new AlertObservation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .alertId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .linkId("linkId1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .alertSeverity("alertSeverity1");
    }

    public static AlertObservation getAlertObservationSample2() {
        return new AlertObservation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .alertId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .linkId("linkId2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .alertSeverity("alertSeverity2");
    }

    public static AlertObservation getAlertObservationRandomSampleGenerator() {
        return new AlertObservation()
            .id(UUID.randomUUID())
            .alertId(UUID.randomUUID())
            .linkId(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .alertSeverity(UUID.randomUUID().toString());
    }
}
