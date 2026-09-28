package cm.gimac.des.reporting.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportedStatementTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ImportedStatement getImportedStatementSample1() {
        return new ImportedStatement()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .period("period1")
            .importerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .digest("digest1")
            .rejectedRows(1)
            .rowCount(1)
            .documentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ImportedStatement getImportedStatementSample2() {
        return new ImportedStatement()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .period("period2")
            .importerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .digest("digest2")
            .rejectedRows(2)
            .rowCount(2)
            .documentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ImportedStatement getImportedStatementRandomSampleGenerator() {
        return new ImportedStatement()
            .id(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .period(UUID.randomUUID().toString())
            .importerId(UUID.randomUUID())
            .digest(UUID.randomUUID().toString())
            .rejectedRows(intCount.incrementAndGet())
            .rowCount(intCount.incrementAndGet())
            .documentRef(UUID.randomUUID());
    }
}
