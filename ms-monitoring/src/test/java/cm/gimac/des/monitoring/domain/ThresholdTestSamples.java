package cm.gimac.des.monitoring.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ThresholdTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Threshold getThresholdSample1() {
        return new Threshold().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).indicator("indicator1").debounce(1);
    }

    public static Threshold getThresholdSample2() {
        return new Threshold().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).indicator("indicator2").debounce(2);
    }

    public static Threshold getThresholdRandomSampleGenerator() {
        return new Threshold().id(UUID.randomUUID()).indicator(UUID.randomUUID().toString()).debounce(intCount.incrementAndGet());
    }
}
