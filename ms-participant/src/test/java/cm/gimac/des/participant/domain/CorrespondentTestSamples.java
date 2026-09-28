package cm.gimac.des.participant.domain;

import java.util.UUID;

public class CorrespondentTestSamples {

    public static Correspondent getCorrespondentSample1() {
        return new Correspondent()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .fullName("fullName1")
            .channels("channels1")
            .email("email1")
            .phone("phone1")
            .publicKeyRef("publicKeyRef1")
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .proposedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Correspondent getCorrespondentSample2() {
        return new Correspondent()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .fullName("fullName2")
            .channels("channels2")
            .email("email2")
            .phone("phone2")
            .publicKeyRef("publicKeyRef2")
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .proposedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Correspondent getCorrespondentRandomSampleGenerator() {
        return new Correspondent()
            .id(UUID.randomUUID())
            .fullName(UUID.randomUUID().toString())
            .channels(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .phone(UUID.randomUUID().toString())
            .publicKeyRef(UUID.randomUUID().toString())
            .userId(UUID.randomUUID())
            .proposedBy(UUID.randomUUID())
            .validatedBy(UUID.randomUUID());
    }
}
