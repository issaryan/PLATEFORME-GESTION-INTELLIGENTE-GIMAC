package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class TestScenarioTestSamples {

    public static TestScenario getTestScenarioSample1() {
        return new TestScenario()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .simulatorScenarioId("simulatorScenarioId1")
            .reason("reason1")
            .traceRef("traceRef1");
    }

    public static TestScenario getTestScenarioSample2() {
        return new TestScenario()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .simulatorScenarioId("simulatorScenarioId2")
            .reason("reason2")
            .traceRef("traceRef2");
    }

    public static TestScenario getTestScenarioRandomSampleGenerator() {
        return new TestScenario()
            .id(UUID.randomUUID())
            .simulatorScenarioId(UUID.randomUUID().toString())
            .reason(UUID.randomUUID().toString())
            .traceRef(UUID.randomUUID().toString());
    }
}
