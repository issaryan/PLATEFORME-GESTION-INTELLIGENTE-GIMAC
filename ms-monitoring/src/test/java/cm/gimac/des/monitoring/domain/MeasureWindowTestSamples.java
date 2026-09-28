package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class MeasureWindowTestSamples {

    public static MeasureWindow getMeasureWindowSample1() {
        return new MeasureWindow().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static MeasureWindow getMeasureWindowSample2() {
        return new MeasureWindow().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static MeasureWindow getMeasureWindowRandomSampleGenerator() {
        return new MeasureWindow().id(UUID.randomUUID());
    }
}
