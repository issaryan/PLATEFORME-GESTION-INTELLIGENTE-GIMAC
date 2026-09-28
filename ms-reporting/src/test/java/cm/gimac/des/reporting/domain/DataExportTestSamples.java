package cm.gimac.des.reporting.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DataExportTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static DataExport getDataExportSample1() {
        return new DataExport()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requesterId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .indicatorCodes("indicatorCodes1")
            .period("period1")
            .rowCount(1)
            .watermark("watermark1")
            .contentDigest("contentDigest1")
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static DataExport getDataExportSample2() {
        return new DataExport()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requesterId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .indicatorCodes("indicatorCodes2")
            .period("period2")
            .rowCount(2)
            .watermark("watermark2")
            .contentDigest("contentDigest2")
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static DataExport getDataExportRandomSampleGenerator() {
        return new DataExport()
            .id(UUID.randomUUID())
            .requesterId(UUID.randomUUID())
            .indicatorCodes(UUID.randomUUID().toString())
            .period(UUID.randomUUID().toString())
            .rowCount(intCount.incrementAndGet())
            .watermark(UUID.randomUUID().toString())
            .contentDigest(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID());
    }
}
