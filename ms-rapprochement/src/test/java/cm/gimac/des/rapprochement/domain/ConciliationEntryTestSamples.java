package cm.gimac.des.rapprochement.domain;

import java.util.UUID;

public class ConciliationEntryTestSamples {

    public static ConciliationEntry getConciliationEntrySample1() {
        return new ConciliationEntry()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .text("text1")
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ConciliationEntry getConciliationEntrySample2() {
        return new ConciliationEntry()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .text("text2")
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ConciliationEntry getConciliationEntryRandomSampleGenerator() {
        return new ConciliationEntry()
            .id(UUID.randomUUID())
            .authorId(UUID.randomUUID())
            .text(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID());
    }
}
