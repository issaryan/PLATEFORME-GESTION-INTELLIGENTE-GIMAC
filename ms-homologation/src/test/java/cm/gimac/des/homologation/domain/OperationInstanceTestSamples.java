package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class OperationInstanceTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static OperationInstance getOperationInstanceSample1() {
        return new OperationInstance()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .libelle("libelle1")
            .serviceCode("serviceCode1")
            .degreEntrant(1);
    }

    public static OperationInstance getOperationInstanceSample2() {
        return new OperationInstance()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .libelle("libelle2")
            .serviceCode("serviceCode2")
            .degreEntrant(2);
    }

    public static OperationInstance getOperationInstanceRandomSampleGenerator() {
        return new OperationInstance()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .libelle(UUID.randomUUID().toString())
            .serviceCode(UUID.randomUUID().toString())
            .degreEntrant(intCount.incrementAndGet());
    }
}
