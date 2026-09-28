package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class AlertSourceTestSamples {

    public static AlertSource getAlertSourceSample1() {
        return new AlertSource()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .certificateThumbprint("certificateThumbprint1");
    }

    public static AlertSource getAlertSourceSample2() {
        return new AlertSource()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .certificateThumbprint("certificateThumbprint2");
    }

    public static AlertSource getAlertSourceRandomSampleGenerator() {
        return new AlertSource()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .certificateThumbprint(UUID.randomUUID().toString());
    }
}
