package cm.gimac.des.notification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsolidationWindowTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ConsolidationWindow getConsolidationWindowSample1() {
        return new ConsolidationWindow().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).groupKey("groupKey1").memberCount(1);
    }

    public static ConsolidationWindow getConsolidationWindowSample2() {
        return new ConsolidationWindow().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).groupKey("groupKey2").memberCount(2);
    }

    public static ConsolidationWindow getConsolidationWindowRandomSampleGenerator() {
        return new ConsolidationWindow()
            .id(UUID.randomUUID())
            .groupKey(UUID.randomUUID().toString())
            .memberCount(intCount.incrementAndGet());
    }
}
