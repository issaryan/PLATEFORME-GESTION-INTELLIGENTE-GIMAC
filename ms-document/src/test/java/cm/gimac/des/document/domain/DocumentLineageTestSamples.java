package cm.gimac.des.document.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentLineageTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static DocumentLineage getDocumentLineageSample1() {
        return new DocumentLineage()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .ownerService("ownerService1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .currentVersion(1);
    }

    public static DocumentLineage getDocumentLineageSample2() {
        return new DocumentLineage()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .ownerService("ownerService2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .currentVersion(2);
    }

    public static DocumentLineage getDocumentLineageRandomSampleGenerator() {
        return new DocumentLineage()
            .id(UUID.randomUUID())
            .ownerService(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .currentVersion(intCount.incrementAndGet());
    }
}
