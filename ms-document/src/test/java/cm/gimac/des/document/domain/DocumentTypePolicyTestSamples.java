package cm.gimac.des.document.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DocumentTypePolicyTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static DocumentTypePolicy getDocumentTypePolicySample1() {
        return new DocumentTypePolicy()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .policyVersion(1)
            .retentionPeriod("retentionPeriod1")
            .allowedMediaTypes("allowedMediaTypes1")
            .maxSizeBytes(1L)
            .allowedCallers("allowedCallers1");
    }

    public static DocumentTypePolicy getDocumentTypePolicySample2() {
        return new DocumentTypePolicy()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .policyVersion(2)
            .retentionPeriod("retentionPeriod2")
            .allowedMediaTypes("allowedMediaTypes2")
            .maxSizeBytes(2L)
            .allowedCallers("allowedCallers2");
    }

    public static DocumentTypePolicy getDocumentTypePolicyRandomSampleGenerator() {
        return new DocumentTypePolicy()
            .id(UUID.randomUUID())
            .policyVersion(intCount.incrementAndGet())
            .retentionPeriod(UUID.randomUUID().toString())
            .allowedMediaTypes(UUID.randomUUID().toString())
            .maxSizeBytes(longCount.incrementAndGet())
            .allowedCallers(UUID.randomUUID().toString());
    }
}
