package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class WipOverrideTestSamples {

    public static WipOverride getWipOverrideSample1() {
        return new WipOverride()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1");
    }

    public static WipOverride getWipOverrideSample2() {
        return new WipOverride()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2");
    }

    public static WipOverride getWipOverrideRandomSampleGenerator() {
        return new WipOverride()
            .id(UUID.randomUUID())
            .requesterId(UUID.randomUUID())
            .approverId(UUID.randomUUID())
            .reason(UUID.randomUUID().toString());
    }
}
