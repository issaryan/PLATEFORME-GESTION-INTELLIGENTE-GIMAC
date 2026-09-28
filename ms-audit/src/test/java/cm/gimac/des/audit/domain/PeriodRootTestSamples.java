package cm.gimac.des.audit.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PeriodRootTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static PeriodRoot getPeriodRootSample1() {
        return new PeriodRoot()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .period("period1")
            .treeSize(1L)
            .rootHash("rootHash1")
            .timestampSerialNumber("timestampSerialNumber1")
            .timestampImprint("timestampImprint1")
            .publicationReference("publicationReference1");
    }

    public static PeriodRoot getPeriodRootSample2() {
        return new PeriodRoot()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .period("period2")
            .treeSize(2L)
            .rootHash("rootHash2")
            .timestampSerialNumber("timestampSerialNumber2")
            .timestampImprint("timestampImprint2")
            .publicationReference("publicationReference2");
    }

    public static PeriodRoot getPeriodRootRandomSampleGenerator() {
        return new PeriodRoot()
            .id(UUID.randomUUID())
            .period(UUID.randomUUID().toString())
            .treeSize(longCount.incrementAndGet())
            .rootHash(UUID.randomUUID().toString())
            .timestampSerialNumber(UUID.randomUUID().toString())
            .timestampImprint(UUID.randomUUID().toString())
            .publicationReference(UUID.randomUUID().toString());
    }
}
