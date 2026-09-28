package cm.gimac.des.iam.domain;

import java.util.UUID;

public class AssignmentTestSamples {

    public static Assignment getAssignmentSample1() {
        return new Assignment().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).department("department1").unit("unit1");
    }

    public static Assignment getAssignmentSample2() {
        return new Assignment().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).department("department2").unit("unit2");
    }

    public static Assignment getAssignmentRandomSampleGenerator() {
        return new Assignment().id(UUID.randomUUID()).department(UUID.randomUUID().toString()).unit(UUID.randomUUID().toString());
    }
}
