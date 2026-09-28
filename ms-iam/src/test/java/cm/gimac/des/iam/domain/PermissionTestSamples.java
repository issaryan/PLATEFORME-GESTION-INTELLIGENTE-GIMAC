package cm.gimac.des.iam.domain;

import java.util.UUID;

public class PermissionTestSamples {

    public static Permission getPermissionSample1() {
        return new Permission()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .resource("resource1")
            .action("action1")
            .targetDomain("targetDomain1");
    }

    public static Permission getPermissionSample2() {
        return new Permission()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .resource("resource2")
            .action("action2")
            .targetDomain("targetDomain2");
    }

    public static Permission getPermissionRandomSampleGenerator() {
        return new Permission()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .resource(UUID.randomUUID().toString())
            .action(UUID.randomUUID().toString())
            .targetDomain(UUID.randomUUID().toString());
    }
}
