package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EvidenceReferenceTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static EvidenceReference getEvidenceReferenceSample1() {
        return new EvidenceReference()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentRef("documentRef1")
            .empreinte("empreinte1")
            .version(1)
            .typePiece("typePiece1")
            .deposantId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static EvidenceReference getEvidenceReferenceSample2() {
        return new EvidenceReference()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentRef("documentRef2")
            .empreinte("empreinte2")
            .version(2)
            .typePiece("typePiece2")
            .deposantId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static EvidenceReference getEvidenceReferenceRandomSampleGenerator() {
        return new EvidenceReference()
            .id(UUID.randomUUID())
            .documentRef(UUID.randomUUID().toString())
            .empreinte(UUID.randomUUID().toString())
            .version(intCount.incrementAndGet())
            .typePiece(UUID.randomUUID().toString())
            .deposantId(UUID.randomUUID());
    }
}
