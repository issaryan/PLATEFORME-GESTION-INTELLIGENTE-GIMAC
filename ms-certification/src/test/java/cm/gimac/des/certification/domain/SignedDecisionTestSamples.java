package cm.gimac.des.certification.domain;

import java.util.UUID;

public class SignedDecisionTestSamples {

    public static SignedDecision getSignedDecisionSample1() {
        return new SignedDecision()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .motif("motif1")
            .signataireId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .signataireRole("signataireRole1")
            .socleEmpreinte("socleEmpreinte1")
            .empreinteDecision("empreinteDecision1")
            .referenceAudit("referenceAudit1");
    }

    public static SignedDecision getSignedDecisionSample2() {
        return new SignedDecision()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .motif("motif2")
            .signataireId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .signataireRole("signataireRole2")
            .socleEmpreinte("socleEmpreinte2")
            .empreinteDecision("empreinteDecision2")
            .referenceAudit("referenceAudit2");
    }

    public static SignedDecision getSignedDecisionRandomSampleGenerator() {
        return new SignedDecision()
            .id(UUID.randomUUID())
            .motif(UUID.randomUUID().toString())
            .signataireId(UUID.randomUUID())
            .signataireRole(UUID.randomUUID().toString())
            .socleEmpreinte(UUID.randomUUID().toString())
            .empreinteDecision(UUID.randomUUID().toString())
            .referenceAudit(UUID.randomUUID().toString());
    }
}
