package cm.gimac.des.certification.domain;

import java.util.UUID;

public class CertificationAttestationTestSamples {

    public static CertificationAttestation getCertificationAttestationSample1() {
        return new CertificationAttestation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .attestationNumber("attestationNumber1")
            .documentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .pdfDigest("pdfDigest1")
            .signatureLevel("signatureLevel1");
    }

    public static CertificationAttestation getCertificationAttestationSample2() {
        return new CertificationAttestation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .attestationNumber("attestationNumber2")
            .documentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .pdfDigest("pdfDigest2")
            .signatureLevel("signatureLevel2");
    }

    public static CertificationAttestation getCertificationAttestationRandomSampleGenerator() {
        return new CertificationAttestation()
            .id(UUID.randomUUID())
            .attestationNumber(UUID.randomUUID().toString())
            .documentId(UUID.randomUUID())
            .pdfDigest(UUID.randomUUID().toString())
            .signatureLevel(UUID.randomUUID().toString());
    }
}
