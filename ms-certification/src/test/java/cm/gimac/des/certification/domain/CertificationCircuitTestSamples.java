package cm.gimac.des.certification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CertificationCircuitTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static CertificationCircuit getCertificationCircuitSample1() {
        return new CertificationCircuit()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .cycle(1)
            .socleEmpreinte("socleEmpreinte1");
    }

    public static CertificationCircuit getCertificationCircuitSample2() {
        return new CertificationCircuit()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .cycle(2)
            .socleEmpreinte("socleEmpreinte2");
    }

    public static CertificationCircuit getCertificationCircuitRandomSampleGenerator() {
        return new CertificationCircuit()
            .id(UUID.randomUUID())
            .liaisonId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .cycle(intCount.incrementAndGet())
            .socleEmpreinte(UUID.randomUUID().toString());
    }
}
