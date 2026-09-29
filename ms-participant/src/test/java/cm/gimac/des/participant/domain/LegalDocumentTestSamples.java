package cm.gimac.des.participant.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class LegalDocumentTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static LegalDocument getLegalDocumentSample1() {
        return new LegalDocument()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .issuingAuthority("issuingAuthority1")
            .documentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentVersion(1)
            .digest("digest1");
    }

    public static LegalDocument getLegalDocumentSample2() {
        return new LegalDocument()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .issuingAuthority("issuingAuthority2")
            .documentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentVersion(2)
            .digest("digest2");
    }

    public static LegalDocument getLegalDocumentRandomSampleGenerator() {
        return new LegalDocument()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .issuingAuthority(UUID.randomUUID().toString())
            .documentId(UUID.randomUUID())
            .documentVersion(intCount.incrementAndGet())
            .digest(UUID.randomUUID().toString());
    }
}
