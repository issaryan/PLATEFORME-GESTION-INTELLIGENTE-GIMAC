package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportedStatementTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ImportedStatement getImportedStatementSample1() {
        return new ImportedStatement()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .importerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .digest("digest1")
            .maskingRejectedLines(1)
            .lineCount(1)
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ImportedStatement getImportedStatementSample2() {
        return new ImportedStatement()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .importerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .digest("digest2")
            .maskingRejectedLines(2)
            .lineCount(2)
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ImportedStatement getImportedStatementRandomSampleGenerator() {
        return new ImportedStatement()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .importerId(UUID.randomUUID())
            .digest(UUID.randomUUID().toString())
            .maskingRejectedLines(intCount.incrementAndGet())
            .lineCount(intCount.incrementAndGet())
            .documentRef(UUID.randomUUID());
    }
}
