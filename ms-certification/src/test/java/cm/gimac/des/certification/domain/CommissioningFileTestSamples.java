package cm.gimac.des.certification.domain;

import java.util.UUID;

public class CommissioningFileTestSamples {

    public static CommissioningFile getCommissioningFileSample1() {
        return new CommissioningFile()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .fileCode("fileCode1")
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .projectLeadId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .basisDigest("basisDigest1")
            .correctionReason("correctionReason1")
            .manifestDigest("manifestDigest1");
    }

    public static CommissioningFile getCommissioningFileSample2() {
        return new CommissioningFile()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .fileCode("fileCode2")
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .projectLeadId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .basisDigest("basisDigest2")
            .correctionReason("correctionReason2")
            .manifestDigest("manifestDigest2");
    }

    public static CommissioningFile getCommissioningFileRandomSampleGenerator() {
        return new CommissioningFile()
            .id(UUID.randomUUID())
            .fileCode(UUID.randomUUID().toString())
            .liaisonId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .projectLeadId(UUID.randomUUID())
            .basisDigest(UUID.randomUUID().toString())
            .correctionReason(UUID.randomUUID().toString())
            .manifestDigest(UUID.randomUUID().toString());
    }
}
