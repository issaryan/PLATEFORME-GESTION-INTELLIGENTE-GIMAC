package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class ProjectionRuleTestSamples {

    public static ProjectionRule getProjectionRuleSample1() {
        return new ProjectionRule()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .subject("subject1")
            .indicatorCodes("indicatorCodes1");
    }

    public static ProjectionRule getProjectionRuleSample2() {
        return new ProjectionRule()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .subject("subject2")
            .indicatorCodes("indicatorCodes2");
    }

    public static ProjectionRule getProjectionRuleRandomSampleGenerator() {
        return new ProjectionRule()
            .id(UUID.randomUUID())
            .subject(UUID.randomUUID().toString())
            .indicatorCodes(UUID.randomUUID().toString());
    }
}
