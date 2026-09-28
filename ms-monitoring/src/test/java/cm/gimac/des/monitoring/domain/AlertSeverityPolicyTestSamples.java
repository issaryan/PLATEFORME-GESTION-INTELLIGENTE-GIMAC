package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class AlertSeverityPolicyTestSamples {

    public static AlertSeverityPolicy getAlertSeverityPolicySample1() {
        return new AlertSeverityPolicy().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static AlertSeverityPolicy getAlertSeverityPolicySample2() {
        return new AlertSeverityPolicy().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static AlertSeverityPolicy getAlertSeverityPolicyRandomSampleGenerator() {
        return new AlertSeverityPolicy().id(UUID.randomUUID());
    }
}
