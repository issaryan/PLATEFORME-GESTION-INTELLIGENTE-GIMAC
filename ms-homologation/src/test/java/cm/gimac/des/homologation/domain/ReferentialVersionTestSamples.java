package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReferentialVersionTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static ReferentialVersion getReferentialVersionSample1() {
        return new ReferentialVersion()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .numero(1)
            .libelle("libelle1")
            .versionSourceId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ReferentialVersion getReferentialVersionSample2() {
        return new ReferentialVersion()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .numero(2)
            .libelle("libelle2")
            .versionSourceId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ReferentialVersion getReferentialVersionRandomSampleGenerator() {
        return new ReferentialVersion()
            .id(UUID.randomUUID())
            .numero(intCount.incrementAndGet())
            .libelle(UUID.randomUUID().toString())
            .versionSourceId(UUID.randomUUID());
    }
}
