package cm.gimac.des.iam.domain;

import java.util.UUID;

public class UserSessionTestSamples {

    public static UserSession getUserSessionSample1() {
        return new UserSession()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .certificateThumbprint("certificateThumbprint1")
            .authMethods("authMethods1")
            .revocationReason("revocationReason1");
    }

    public static UserSession getUserSessionSample2() {
        return new UserSession()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .certificateThumbprint("certificateThumbprint2")
            .authMethods("authMethods2")
            .revocationReason("revocationReason2");
    }

    public static UserSession getUserSessionRandomSampleGenerator() {
        return new UserSession()
            .id(UUID.randomUUID())
            .certificateThumbprint(UUID.randomUUID().toString())
            .authMethods(UUID.randomUUID().toString())
            .revocationReason(UUID.randomUUID().toString());
    }
}
