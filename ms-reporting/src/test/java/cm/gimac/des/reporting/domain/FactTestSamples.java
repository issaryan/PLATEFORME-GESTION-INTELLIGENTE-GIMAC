package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class FactTestSamples {

    public static Fact getFactSample1() {
        return new Fact()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .severity("severity1")
            .sourceEventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Fact getFactSample2() {
        return new Fact()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .severity("severity2")
            .sourceEventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Fact getFactRandomSampleGenerator() {
        return new Fact()
            .id(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .severity(UUID.randomUUID().toString())
            .sourceEventId(UUID.randomUUID());
    }
}
