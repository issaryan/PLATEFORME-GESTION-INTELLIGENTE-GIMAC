package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class CampaignResultTestSamples {

    public static CampaignResult getCampaignResultSample1() {
        return new CampaignResult()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .campagneId("campagneId1")
            .executionId("executionId1")
            .idScenario("idScenario1")
            .versionScenario("versionScenario1")
            .referenceTrace("referenceTrace1")
            .empreinteReponse("empreinteReponse1");
    }

    public static CampaignResult getCampaignResultSample2() {
        return new CampaignResult()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .campagneId("campagneId2")
            .executionId("executionId2")
            .idScenario("idScenario2")
            .versionScenario("versionScenario2")
            .referenceTrace("referenceTrace2")
            .empreinteReponse("empreinteReponse2");
    }

    public static CampaignResult getCampaignResultRandomSampleGenerator() {
        return new CampaignResult()
            .id(UUID.randomUUID())
            .campagneId(UUID.randomUUID().toString())
            .executionId(UUID.randomUUID().toString())
            .idScenario(UUID.randomUUID().toString())
            .versionScenario(UUID.randomUUID().toString())
            .referenceTrace(UUID.randomUUID().toString())
            .empreinteReponse(UUID.randomUUID().toString());
    }
}
