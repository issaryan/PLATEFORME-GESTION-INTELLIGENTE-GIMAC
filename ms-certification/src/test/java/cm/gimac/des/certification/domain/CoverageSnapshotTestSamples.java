package cm.gimac.des.certification.domain;

import java.util.UUID;

public class CoverageSnapshotTestSamples {

    public static CoverageSnapshot getCoverageSnapshotSample1() {
        return new CoverageSnapshot()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .sourceEventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static CoverageSnapshot getCoverageSnapshotSample2() {
        return new CoverageSnapshot()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .sourceEventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static CoverageSnapshot getCoverageSnapshotRandomSampleGenerator() {
        return new CoverageSnapshot()
            .id(UUID.randomUUID())
            .liaisonId(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .sourceEventId(UUID.randomUUID());
    }
}
