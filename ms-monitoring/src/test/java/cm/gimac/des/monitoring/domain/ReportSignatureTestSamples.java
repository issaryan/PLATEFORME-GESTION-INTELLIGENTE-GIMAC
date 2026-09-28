package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class ReportSignatureTestSamples {

    public static ReportSignature getReportSignatureSample1() {
        return new ReportSignature()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .signerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .challenge("challenge1")
            .reservations("reservations1");
    }

    public static ReportSignature getReportSignatureSample2() {
        return new ReportSignature()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .signerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .challenge("challenge2")
            .reservations("reservations2");
    }

    public static ReportSignature getReportSignatureRandomSampleGenerator() {
        return new ReportSignature()
            .id(UUID.randomUUID())
            .signerId(UUID.randomUUID())
            .challenge(UUID.randomUUID().toString())
            .reservations(UUID.randomUUID().toString());
    }
}
