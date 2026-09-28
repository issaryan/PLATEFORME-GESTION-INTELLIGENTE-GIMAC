package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class SupervisedLinkTestSamples {

    public static SupervisedLink getSupervisedLinkSample1() {
        return new SupervisedLink()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .type("type1");
    }

    public static SupervisedLink getSupervisedLinkSample2() {
        return new SupervisedLink()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .type("type2");
    }

    public static SupervisedLink getSupervisedLinkRandomSampleGenerator() {
        return new SupervisedLink()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .type(UUID.randomUUID().toString());
    }
}
