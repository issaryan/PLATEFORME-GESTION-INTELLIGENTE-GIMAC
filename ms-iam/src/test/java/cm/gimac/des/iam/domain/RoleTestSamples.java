package cm.gimac.des.iam.domain;

import java.util.UUID;

public class RoleTestSamples {

    public static Role getRoleSample1() {
        return new Role()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .directoryGroup("directoryGroup1");
    }

    public static Role getRoleSample2() {
        return new Role()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .directoryGroup("directoryGroup2");
    }

    public static Role getRoleRandomSampleGenerator() {
        return new Role()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .directoryGroup(UUID.randomUUID().toString());
    }
}
