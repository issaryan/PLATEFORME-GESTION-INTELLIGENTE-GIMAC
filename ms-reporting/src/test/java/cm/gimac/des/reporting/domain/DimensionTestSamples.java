package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class DimensionTestSamples {

    public static Dimension getDimensionSample1() {
        return new Dimension()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .key("key1")
            .label("label1")
            .parentKey("parentKey1");
    }

    public static Dimension getDimensionSample2() {
        return new Dimension()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .key("key2")
            .label("label2")
            .parentKey("parentKey2");
    }

    public static Dimension getDimensionRandomSampleGenerator() {
        return new Dimension()
            .id(UUID.randomUUID())
            .key(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .parentKey(UUID.randomUUID().toString());
    }
}
