package cm.gimac.des.document.domain;

import java.util.UUID;

public class IntegrityMismatchTestSamples {

    public static IntegrityMismatch getIntegrityMismatchSample1() {
        return new IntegrityMismatch()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .objectKey("objectKey1")
            .expectedSha256("expectedSha2561")
            .actualSha256("actualSha2561");
    }

    public static IntegrityMismatch getIntegrityMismatchSample2() {
        return new IntegrityMismatch()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .objectKey("objectKey2")
            .expectedSha256("expectedSha2562")
            .actualSha256("actualSha2562");
    }

    public static IntegrityMismatch getIntegrityMismatchRandomSampleGenerator() {
        return new IntegrityMismatch()
            .id(UUID.randomUUID())
            .objectKey(UUID.randomUUID().toString())
            .expectedSha256(UUID.randomUUID().toString())
            .actualSha256(UUID.randomUUID().toString());
    }
}
