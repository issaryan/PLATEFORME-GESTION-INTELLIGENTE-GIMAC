package cm.gimac.des.reporting.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class IndicatorDefinitionTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static IndicatorDefinition getIndicatorDefinitionSample1() {
        return new IndicatorDefinition()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .unit("unit1")
            .segmentCount(1)
            .audience("audience1");
    }

    public static IndicatorDefinition getIndicatorDefinitionSample2() {
        return new IndicatorDefinition()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .unit("unit2")
            .segmentCount(2)
            .audience("audience2");
    }

    public static IndicatorDefinition getIndicatorDefinitionRandomSampleGenerator() {
        return new IndicatorDefinition()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .unit(UUID.randomUUID().toString())
            .segmentCount(intCount.incrementAndGet())
            .audience(UUID.randomUUID().toString());
    }
}
