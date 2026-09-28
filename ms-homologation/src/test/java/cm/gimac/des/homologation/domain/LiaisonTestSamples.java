package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class LiaisonTestSamples {

    public static Liaison getLiaisonSample1() {
        return new Liaison()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .liaisonCode("liaisonCode1")
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .projectLeadId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .campaignRef("campaignRef1")
            .stateReason("stateReason1");
    }

    public static Liaison getLiaisonSample2() {
        return new Liaison()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .liaisonCode("liaisonCode2")
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .projectLeadId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .campaignRef("campaignRef2")
            .stateReason("stateReason2");
    }

    public static Liaison getLiaisonRandomSampleGenerator() {
        return new Liaison()
            .id(UUID.randomUUID())
            .liaisonCode(UUID.randomUUID().toString())
            .institutionId(UUID.randomUUID())
            .projectLeadId(UUID.randomUUID())
            .campaignRef(UUID.randomUUID().toString())
            .stateReason(UUID.randomUUID().toString());
    }
}
