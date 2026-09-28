package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AgingRuleTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AgingRule getAgingRuleSample1() {
        return new AgingRule().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).thresholdDays(1).recipientRole("recipientRole1");
    }

    public static AgingRule getAgingRuleSample2() {
        return new AgingRule().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).thresholdDays(2).recipientRole("recipientRole2");
    }

    public static AgingRule getAgingRuleRandomSampleGenerator() {
        return new AgingRule().id(UUID.randomUUID()).thresholdDays(intCount.incrementAndGet()).recipientRole(UUID.randomUUID().toString());
    }
}
