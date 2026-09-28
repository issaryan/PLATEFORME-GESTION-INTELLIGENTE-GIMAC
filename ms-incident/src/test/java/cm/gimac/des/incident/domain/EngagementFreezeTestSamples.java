package cm.gimac.des.incident.domain;

import java.util.UUID;

public class EngagementFreezeTestSamples {

    public static EngagementFreeze getEngagementFreezeSample1() {
        return new EngagementFreeze()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .thirdParty("thirdParty1")
            .proofRef("proofRef1");
    }

    public static EngagementFreeze getEngagementFreezeSample2() {
        return new EngagementFreeze()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .thirdParty("thirdParty2")
            .proofRef("proofRef2");
    }

    public static EngagementFreeze getEngagementFreezeRandomSampleGenerator() {
        return new EngagementFreeze()
            .id(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .thirdParty(UUID.randomUUID().toString())
            .proofRef(UUID.randomUUID().toString());
    }
}
