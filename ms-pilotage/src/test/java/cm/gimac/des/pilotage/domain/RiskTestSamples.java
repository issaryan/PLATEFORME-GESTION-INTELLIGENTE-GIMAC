package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RiskTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Risk getRiskSample1() {
        return new Risk()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .scope("scope1")
            .description("description1")
            .probability(1)
            .impact(1)
            .grossScore(1)
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .mitigationPlan("mitigationPlan1");
    }

    public static Risk getRiskSample2() {
        return new Risk()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .scope("scope2")
            .description("description2")
            .probability(2)
            .impact(2)
            .grossScore(2)
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .mitigationPlan("mitigationPlan2");
    }

    public static Risk getRiskRandomSampleGenerator() {
        return new Risk()
            .id(UUID.randomUUID())
            .scope(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .probability(intCount.incrementAndGet())
            .impact(intCount.incrementAndGet())
            .grossScore(intCount.incrementAndGet())
            .ownerId(UUID.randomUUID())
            .mitigationPlan(UUID.randomUUID().toString());
    }
}
