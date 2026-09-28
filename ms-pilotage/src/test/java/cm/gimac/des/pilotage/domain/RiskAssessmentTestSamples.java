package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RiskAssessmentTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static RiskAssessment getRiskAssessmentSample1() {
        return new RiskAssessment()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .probability(1)
            .impact(1)
            .grossScore(1)
            .assessorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static RiskAssessment getRiskAssessmentSample2() {
        return new RiskAssessment()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .probability(2)
            .impact(2)
            .grossScore(2)
            .assessorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static RiskAssessment getRiskAssessmentRandomSampleGenerator() {
        return new RiskAssessment()
            .id(UUID.randomUUID())
            .probability(intCount.incrementAndGet())
            .impact(intCount.incrementAndGet())
            .grossScore(intCount.incrementAndGet())
            .assessorId(UUID.randomUUID());
    }
}
