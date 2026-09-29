package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ScopeItemTestSamples {

    public static ScopeItem getScopeItemSample1() {
        return new ScopeItem()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .serviceLibelle("serviceLibelle1");
    }

    public static ScopeItem getScopeItemSample2() {
        return new ScopeItem()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .serviceLibelle("serviceLibelle2");
    }

    public static ScopeItem getScopeItemRandomSampleGenerator() {
        return new ScopeItem().id(UUID.randomUUID()).serviceCode(UUID.randomUUID().toString()).serviceLibelle(UUID.randomUUID().toString());
    }
}
