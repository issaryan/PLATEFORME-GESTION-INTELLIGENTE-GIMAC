package cm.gimac.des.certification.domain;

import java.util.UUID;

public class ChangeRecordTestSamples {

    public static ChangeRecord getChangeRecordSample1() {
        return new ChangeRecord()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .externalRef("externalRef1")
            .contentDigest("contentDigest1")
            .referencedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ChangeRecord getChangeRecordSample2() {
        return new ChangeRecord()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .externalRef("externalRef2")
            .contentDigest("contentDigest2")
            .referencedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ChangeRecord getChangeRecordRandomSampleGenerator() {
        return new ChangeRecord()
            .id(UUID.randomUUID())
            .externalRef(UUID.randomUUID().toString())
            .contentDigest(UUID.randomUUID().toString())
            .referencedBy(UUID.randomUUID());
    }
}
