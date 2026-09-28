package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class ShiftTestSamples {

    public static Shift getShiftSample1() {
        return new Shift()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .operatorIds("operatorIds1")
            .shiftLeadId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Shift getShiftSample2() {
        return new Shift()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .operatorIds("operatorIds2")
            .shiftLeadId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Shift getShiftRandomSampleGenerator() {
        return new Shift()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .operatorIds(UUID.randomUUID().toString())
            .shiftLeadId(UUID.randomUUID());
    }
}
