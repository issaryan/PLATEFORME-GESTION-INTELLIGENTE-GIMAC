package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class ReportVisaTestSamples {

    public static ReportVisa getReportVisaSample1() {
        return new ReportVisa()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .comment("comment1");
    }

    public static ReportVisa getReportVisaSample2() {
        return new ReportVisa()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .comment("comment2");
    }

    public static ReportVisa getReportVisaRandomSampleGenerator() {
        return new ReportVisa().id(UUID.randomUUID()).approverId(UUID.randomUUID()).comment(UUID.randomUUID().toString());
    }
}
