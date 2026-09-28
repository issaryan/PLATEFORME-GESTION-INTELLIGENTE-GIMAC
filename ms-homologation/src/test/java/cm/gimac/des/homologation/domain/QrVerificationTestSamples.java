package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class QrVerificationTestSamples {

    public static QrVerification getQrVerificationSample1() {
        return new QrVerification()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .verifierId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDocumentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .evidenceDigest("evidenceDigest1");
    }

    public static QrVerification getQrVerificationSample2() {
        return new QrVerification()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .verifierId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDocumentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .evidenceDigest("evidenceDigest2");
    }

    public static QrVerification getQrVerificationRandomSampleGenerator() {
        return new QrVerification()
            .id(UUID.randomUUID())
            .verifierId(UUID.randomUUID())
            .evidenceDocumentId(UUID.randomUUID())
            .evidenceDigest(UUID.randomUUID().toString());
    }
}
