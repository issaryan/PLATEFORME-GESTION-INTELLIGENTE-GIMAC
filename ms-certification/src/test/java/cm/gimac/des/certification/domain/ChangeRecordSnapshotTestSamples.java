package cm.gimac.des.certification.domain;

import java.util.UUID;

public class ChangeRecordSnapshotTestSamples {

    public static ChangeRecordSnapshot getChangeRecordSnapshotSample1() {
        return new ChangeRecordSnapshot()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .etatApprobation("etatApprobation1")
            .empreinteReponse("empreinteReponse1");
    }

    public static ChangeRecordSnapshot getChangeRecordSnapshotSample2() {
        return new ChangeRecordSnapshot()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .etatApprobation("etatApprobation2")
            .empreinteReponse("empreinteReponse2");
    }

    public static ChangeRecordSnapshot getChangeRecordSnapshotRandomSampleGenerator() {
        return new ChangeRecordSnapshot()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .etatApprobation(UUID.randomUUID().toString())
            .empreinteReponse(UUID.randomUUID().toString());
    }
}
