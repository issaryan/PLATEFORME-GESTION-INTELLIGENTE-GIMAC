package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class IndicatorValueTestSamples {

    public static IndicatorValue getIndicatorValueSample1() {
        return new IndicatorValue()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .period("period1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .severity("severity1");
    }

    public static IndicatorValue getIndicatorValueSample2() {
        return new IndicatorValue()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .period("period2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .severity("severity2");
    }

    public static IndicatorValue getIndicatorValueRandomSampleGenerator() {
        return new IndicatorValue()
            .id(UUID.randomUUID())
            .period(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .severity(UUID.randomUUID().toString());
    }
}
