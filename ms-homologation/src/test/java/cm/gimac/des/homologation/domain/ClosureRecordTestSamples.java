package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ClosureRecordTestSamples {

    public static ClosureRecord getClosureRecordSample1() {
        return new ClosureRecord()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validateurId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .empreinteCloture("empreinteCloture1")
            .motifReouverture("motifReouverture1");
    }

    public static ClosureRecord getClosureRecordSample2() {
        return new ClosureRecord()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validateurId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .empreinteCloture("empreinteCloture2")
            .motifReouverture("motifReouverture2");
    }

    public static ClosureRecord getClosureRecordRandomSampleGenerator() {
        return new ClosureRecord()
            .id(UUID.randomUUID())
            .validateurId(UUID.randomUUID())
            .empreinteCloture(UUID.randomUUID().toString())
            .motifReouverture(UUID.randomUUID().toString());
    }
}
