package cm.gimac.des.certification.domain;

import java.util.UUID;

public class GoLiveReportTestSamples {

    public static GoLiveReport getGoLiveReportSample1() {
        return new GoLiveReport()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentRef("documentRef1")
            .empreinte("empreinte1")
            .deposantId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static GoLiveReport getGoLiveReportSample2() {
        return new GoLiveReport()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentRef("documentRef2")
            .empreinte("empreinte2")
            .deposantId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static GoLiveReport getGoLiveReportRandomSampleGenerator() {
        return new GoLiveReport()
            .id(UUID.randomUUID())
            .documentRef(UUID.randomUUID().toString())
            .empreinte(UUID.randomUUID().toString())
            .deposantId(UUID.randomUUID());
    }
}
