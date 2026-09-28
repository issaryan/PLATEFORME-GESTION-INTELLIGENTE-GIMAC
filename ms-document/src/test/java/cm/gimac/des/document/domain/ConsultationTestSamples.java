package cm.gimac.des.document.domain;

import java.util.UUID;

public class ConsultationTestSamples {

    public static Consultation getConsultationSample1() {
        return new Consultation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .correlationId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .refusalReason("refusalReason1");
    }

    public static Consultation getConsultationSample2() {
        return new Consultation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .correlationId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .refusalReason("refusalReason2");
    }

    public static Consultation getConsultationRandomSampleGenerator() {
        return new Consultation()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .correlationId(UUID.randomUUID())
            .refusalReason(UUID.randomUUID().toString());
    }
}
