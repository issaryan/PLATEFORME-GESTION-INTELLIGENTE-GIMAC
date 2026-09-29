package cm.gimac.des.certification.domain;

import java.util.UUID;

public class AttestationTestSamples {

    public static Attestation getAttestationSample1() {
        return new Attestation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .numero("numero1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .typeProjet("typeProjet1")
            .socleEmpreinte("socleEmpreinte1")
            .documentRef("documentRef1")
            .empreinteDocument("empreinteDocument1")
            .referenceAudit("referenceAudit1");
    }

    public static Attestation getAttestationSample2() {
        return new Attestation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .numero("numero2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .typeProjet("typeProjet2")
            .socleEmpreinte("socleEmpreinte2")
            .documentRef("documentRef2")
            .empreinteDocument("empreinteDocument2")
            .referenceAudit("referenceAudit2");
    }

    public static Attestation getAttestationRandomSampleGenerator() {
        return new Attestation()
            .id(UUID.randomUUID())
            .numero(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .typeProjet(UUID.randomUUID().toString())
            .socleEmpreinte(UUID.randomUUID().toString())
            .documentRef(UUID.randomUUID().toString())
            .empreinteDocument(UUID.randomUUID().toString())
            .referenceAudit(UUID.randomUUID().toString());
    }
}
