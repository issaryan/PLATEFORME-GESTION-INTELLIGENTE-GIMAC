package cm.gimac.des.monitoring.domain;

import java.util.UUID;

public class ShiftReportTestSamples {

    public static ShiftReport getShiftReportSample1() {
        return new ShiftReport()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .logHeadHash("logHeadHash1")
            .contentHash("contentHash1")
            .digest("digest1")
            .documentDigest("documentDigest1")
            .timestampSerialNumber("timestampSerialNumber1")
            .documentRef("documentRef1");
    }

    public static ShiftReport getShiftReportSample2() {
        return new ShiftReport()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .logHeadHash("logHeadHash2")
            .contentHash("contentHash2")
            .digest("digest2")
            .documentDigest("documentDigest2")
            .timestampSerialNumber("timestampSerialNumber2")
            .documentRef("documentRef2");
    }

    public static ShiftReport getShiftReportRandomSampleGenerator() {
        return new ShiftReport()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .logHeadHash(UUID.randomUUID().toString())
            .contentHash(UUID.randomUUID().toString())
            .digest(UUID.randomUUID().toString())
            .documentDigest(UUID.randomUUID().toString())
            .timestampSerialNumber(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID().toString());
    }
}
