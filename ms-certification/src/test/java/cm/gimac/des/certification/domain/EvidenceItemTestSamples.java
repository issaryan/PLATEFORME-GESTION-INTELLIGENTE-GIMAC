package cm.gimac.des.certification.domain;

import java.util.UUID;

public class EvidenceItemTestSamples {

    public static EvidenceItem getEvidenceItemSample1() {
        return new EvidenceItem()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .digest("digest1")
            .attachedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static EvidenceItem getEvidenceItemSample2() {
        return new EvidenceItem()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .digest("digest2")
            .attachedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static EvidenceItem getEvidenceItemRandomSampleGenerator() {
        return new EvidenceItem()
            .id(UUID.randomUUID())
            .documentId(UUID.randomUUID())
            .digest(UUID.randomUUID().toString())
            .attachedBy(UUID.randomUUID());
    }
}
