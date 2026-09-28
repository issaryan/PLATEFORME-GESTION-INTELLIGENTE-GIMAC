package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class CollaboratorProjectionTestSamples {

    public static CollaboratorProjection getCollaboratorProjectionSample1() {
        return new CollaboratorProjection()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .collaboratorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .displayName("displayName1")
            .serviceCode("serviceCode1");
    }

    public static CollaboratorProjection getCollaboratorProjectionSample2() {
        return new CollaboratorProjection()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .collaboratorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .displayName("displayName2")
            .serviceCode("serviceCode2");
    }

    public static CollaboratorProjection getCollaboratorProjectionRandomSampleGenerator() {
        return new CollaboratorProjection()
            .id(UUID.randomUUID())
            .collaboratorId(UUID.randomUUID())
            .displayName(UUID.randomUUID().toString())
            .serviceCode(UUID.randomUUID().toString());
    }
}
