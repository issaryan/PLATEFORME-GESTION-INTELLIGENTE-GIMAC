package cm.gimac.des.certification.domain;

import java.util.UUID;

public class CompletenessConditionTestSamples {

    public static CompletenessCondition getCompletenessConditionSample1() {
        return new CompletenessCondition().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).source("source1").detail("detail1");
    }

    public static CompletenessCondition getCompletenessConditionSample2() {
        return new CompletenessCondition().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).source("source2").detail("detail2");
    }

    public static CompletenessCondition getCompletenessConditionRandomSampleGenerator() {
        return new CompletenessCondition().id(UUID.randomUUID()).source(UUID.randomUUID().toString()).detail(UUID.randomUUID().toString());
    }
}
