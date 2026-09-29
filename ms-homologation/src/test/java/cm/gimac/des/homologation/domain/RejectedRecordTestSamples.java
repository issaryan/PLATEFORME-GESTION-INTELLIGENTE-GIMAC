package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class RejectedRecordTestSamples {

    public static RejectedRecord getRejectedRecordSample1() {
        return new RejectedRecord()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .source("source1")
            .empreinte("empreinte1")
            .motif("motif1");
    }

    public static RejectedRecord getRejectedRecordSample2() {
        return new RejectedRecord()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .source("source2")
            .empreinte("empreinte2")
            .motif("motif2");
    }

    public static RejectedRecord getRejectedRecordRandomSampleGenerator() {
        return new RejectedRecord()
            .id(UUID.randomUUID())
            .source(UUID.randomUUID().toString())
            .empreinte(UUID.randomUUID().toString())
            .motif(UUID.randomUUID().toString());
    }
}
