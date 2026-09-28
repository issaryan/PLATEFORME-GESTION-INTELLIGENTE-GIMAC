package cm.gimac.des.reporting.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PeriodicReportTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static PeriodicReport getPeriodicReportSample1() {
        return new PeriodicReport()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .period("period1")
            .reportVersion(1)
            .contentDigest("contentDigest1")
            .signerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static PeriodicReport getPeriodicReportSample2() {
        return new PeriodicReport()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .period("period2")
            .reportVersion(2)
            .contentDigest("contentDigest2")
            .signerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static PeriodicReport getPeriodicReportRandomSampleGenerator() {
        return new PeriodicReport()
            .id(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .period(UUID.randomUUID().toString())
            .reportVersion(intCount.incrementAndGet())
            .contentDigest(UUID.randomUUID().toString())
            .signerId(UUID.randomUUID())
            .documentRef(UUID.randomUUID());
    }
}
