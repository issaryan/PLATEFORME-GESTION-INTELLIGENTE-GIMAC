package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class CockpitViewTestSamples {

    public static CockpitView getCockpitViewSample1() {
        return new CockpitView()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .audience("audience1");
    }

    public static CockpitView getCockpitViewSample2() {
        return new CockpitView()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .audience("audience2");
    }

    public static CockpitView getCockpitViewRandomSampleGenerator() {
        return new CockpitView()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .audience(UUID.randomUUID().toString());
    }
}
