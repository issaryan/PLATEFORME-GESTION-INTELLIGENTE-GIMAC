package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconciliationTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Reconciliation getReconciliationSample1() {
        return new Reconciliation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .matchedCount(1)
            .discrepancyCount(1)
            .closedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Reconciliation getReconciliationSample2() {
        return new Reconciliation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .matchedCount(2)
            .discrepancyCount(2)
            .closedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Reconciliation getReconciliationRandomSampleGenerator() {
        return new Reconciliation()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .matchedCount(intCount.incrementAndGet())
            .discrepancyCount(intCount.incrementAndGet())
            .closedBy(UUID.randomUUID());
    }
}
