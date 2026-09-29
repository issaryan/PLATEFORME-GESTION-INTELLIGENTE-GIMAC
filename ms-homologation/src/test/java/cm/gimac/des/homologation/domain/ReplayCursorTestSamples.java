package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplayCursorTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static ReplayCursor getReplayCursorSample1() {
        return new ReplayCursor()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .campagneId("campagneId1")
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .position("position1")
            .echecsConsecutifs(1);
    }

    public static ReplayCursor getReplayCursorSample2() {
        return new ReplayCursor()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .campagneId("campagneId2")
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .position("position2")
            .echecsConsecutifs(2);
    }

    public static ReplayCursor getReplayCursorRandomSampleGenerator() {
        return new ReplayCursor()
            .id(UUID.randomUUID())
            .campagneId(UUID.randomUUID().toString())
            .liaisonId(UUID.randomUUID())
            .position(UUID.randomUUID().toString())
            .echecsConsecutifs(intCount.incrementAndGet());
    }
}
