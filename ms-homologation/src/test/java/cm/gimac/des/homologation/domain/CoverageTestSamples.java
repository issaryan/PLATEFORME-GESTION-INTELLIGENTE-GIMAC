package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CoverageTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Coverage getCoverageSample1() {
        return new Coverage()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .numerateur(1)
            .denominateur(1)
            .motifNonCalculable("motifNonCalculable1");
    }

    public static Coverage getCoverageSample2() {
        return new Coverage()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .numerateur(2)
            .denominateur(2)
            .motifNonCalculable("motifNonCalculable2");
    }

    public static Coverage getCoverageRandomSampleGenerator() {
        return new Coverage()
            .id(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .numerateur(intCount.incrementAndGet())
            .denominateur(intCount.incrementAndGet())
            .motifNonCalculable(UUID.randomUUID().toString());
    }
}
