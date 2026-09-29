package cm.gimac.des.certification.domain;

import java.util.UUID;

public class AttestationStatusDecisionTestSamples {

    public static AttestationStatusDecision getAttestationStatusDecisionSample1() {
        return new AttestationStatusDecision()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .motif("motif1")
            .pieceRef("pieceRef1")
            .demandeurId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approbateurId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .etat("etat1")
            .avisDocumentRef("avisDocumentRef1")
            .conditionLevee("conditionLevee1");
    }

    public static AttestationStatusDecision getAttestationStatusDecisionSample2() {
        return new AttestationStatusDecision()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .motif("motif2")
            .pieceRef("pieceRef2")
            .demandeurId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approbateurId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .etat("etat2")
            .avisDocumentRef("avisDocumentRef2")
            .conditionLevee("conditionLevee2");
    }

    public static AttestationStatusDecision getAttestationStatusDecisionRandomSampleGenerator() {
        return new AttestationStatusDecision()
            .id(UUID.randomUUID())
            .motif(UUID.randomUUID().toString())
            .pieceRef(UUID.randomUUID().toString())
            .demandeurId(UUID.randomUUID())
            .approbateurId(UUID.randomUUID())
            .etat(UUID.randomUUID().toString())
            .avisDocumentRef(UUID.randomUUID().toString())
            .conditionLevee(UUID.randomUUID().toString());
    }
}
