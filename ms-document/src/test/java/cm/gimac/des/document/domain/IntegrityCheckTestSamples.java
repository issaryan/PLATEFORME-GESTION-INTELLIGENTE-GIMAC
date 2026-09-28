package cm.gimac.des.document.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class IntegrityCheckTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static IntegrityCheck getIntegrityCheckSample1() {
        return new IntegrityCheck().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).sampleSize(1).mismatches(1).unreadable(1);
    }

    public static IntegrityCheck getIntegrityCheckSample2() {
        return new IntegrityCheck().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).sampleSize(2).mismatches(2).unreadable(2);
    }

    public static IntegrityCheck getIntegrityCheckRandomSampleGenerator() {
        return new IntegrityCheck()
            .id(UUID.randomUUID())
            .sampleSize(intCount.incrementAndGet())
            .mismatches(intCount.incrementAndGet())
            .unreadable(intCount.incrementAndGet());
    }
}
