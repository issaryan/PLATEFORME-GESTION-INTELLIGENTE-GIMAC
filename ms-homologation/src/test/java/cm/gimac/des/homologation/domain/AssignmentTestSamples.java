package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class AssignmentTestSamples {

    public static Assignment getAssignmentSample1() {
        return new Assignment()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .responsableId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .responsableNom("responsableNom1")
            .motif("motif1");
    }

    public static Assignment getAssignmentSample2() {
        return new Assignment()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .responsableId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .responsableNom("responsableNom2")
            .motif("motif2");
    }

    public static Assignment getAssignmentRandomSampleGenerator() {
        return new Assignment()
            .id(UUID.randomUUID())
            .responsableId(UUID.randomUUID())
            .responsableNom(UUID.randomUUID().toString())
            .motif(UUID.randomUUID().toString());
    }
}
