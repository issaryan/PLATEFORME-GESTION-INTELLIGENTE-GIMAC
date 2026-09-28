package cm.gimac.des.audit.domain;

import java.util.UUID;

public class VerificationRecordTestSamples {

    public static VerificationRecord getVerificationRecordSample1() {
        return new VerificationRecord()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .verifierId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .expectedRoot("expectedRoot1")
            .recomputedRoot("recomputedRoot1")
            .comment("comment1");
    }

    public static VerificationRecord getVerificationRecordSample2() {
        return new VerificationRecord()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .verifierId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .expectedRoot("expectedRoot2")
            .recomputedRoot("recomputedRoot2")
            .comment("comment2");
    }

    public static VerificationRecord getVerificationRecordRandomSampleGenerator() {
        return new VerificationRecord()
            .id(UUID.randomUUID())
            .verifierId(UUID.randomUUID())
            .expectedRoot(UUID.randomUUID().toString())
            .recomputedRoot(UUID.randomUUID().toString())
            .comment(UUID.randomUUID().toString());
    }
}
