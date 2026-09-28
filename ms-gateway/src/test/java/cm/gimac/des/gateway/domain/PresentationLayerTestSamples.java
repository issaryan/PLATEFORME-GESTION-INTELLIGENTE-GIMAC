package cm.gimac.des.gateway.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PresentationLayerTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static PresentationLayer getPresentationLayerSample1() {
        return new PresentationLayer()
            .id(1L)
            .name("name1")
            .authenticationRequirement("authenticationRequirement1")
            .bffPrefix("bffPrefix1")
            .defaultRateLimitPerMinute(1);
    }

    public static PresentationLayer getPresentationLayerSample2() {
        return new PresentationLayer()
            .id(2L)
            .name("name2")
            .authenticationRequirement("authenticationRequirement2")
            .bffPrefix("bffPrefix2")
            .defaultRateLimitPerMinute(2);
    }

    public static PresentationLayer getPresentationLayerRandomSampleGenerator() {
        return new PresentationLayer()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .authenticationRequirement(UUID.randomUUID().toString())
            .bffPrefix(UUID.randomUUID().toString())
            .defaultRateLimitPerMinute(intCount.incrementAndGet());
    }
}
