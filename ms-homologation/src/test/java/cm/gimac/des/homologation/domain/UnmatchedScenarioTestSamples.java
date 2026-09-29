package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class UnmatchedScenarioTestSamples {

    public static UnmatchedScenario getUnmatchedScenarioSample1() {
        return new UnmatchedScenario()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .idScenario("idScenario1")
            .campagneId("campagneId1")
            .motif("motif1");
    }

    public static UnmatchedScenario getUnmatchedScenarioSample2() {
        return new UnmatchedScenario()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .idScenario("idScenario2")
            .campagneId("campagneId2")
            .motif("motif2");
    }

    public static UnmatchedScenario getUnmatchedScenarioRandomSampleGenerator() {
        return new UnmatchedScenario()
            .id(UUID.randomUUID())
            .idScenario(UUID.randomUUID().toString())
            .campagneId(UUID.randomUUID().toString())
            .motif(UUID.randomUUID().toString());
    }
}
