package cm.gimac.des.iam.domain;

import java.util.UUID;

public class RoleGrantTestSamples {

    public static RoleGrant getRoleGrantSample1() {
        return new RoleGrant()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requestedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approvedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .revocationReason("revocationReason1");
    }

    public static RoleGrant getRoleGrantSample2() {
        return new RoleGrant()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requestedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approvedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .revocationReason("revocationReason2");
    }

    public static RoleGrant getRoleGrantRandomSampleGenerator() {
        return new RoleGrant()
            .id(UUID.randomUUID())
            .requestedBy(UUID.randomUUID())
            .approvedBy(UUID.randomUUID())
            .revocationReason(UUID.randomUUID().toString());
    }
}
