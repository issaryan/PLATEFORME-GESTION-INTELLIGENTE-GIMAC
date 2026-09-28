package cm.gimac.des.incident.domain;

import java.util.UUID;

public class RemediationActionTestSamples {

    public static RemediationAction getRemediationActionSample1() {
        return new RemediationAction()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .description("description1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .taskRef("taskRef1");
    }

    public static RemediationAction getRemediationActionSample2() {
        return new RemediationAction()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .description("description2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .taskRef("taskRef2");
    }

    public static RemediationAction getRemediationActionRandomSampleGenerator() {
        return new RemediationAction()
            .id(UUID.randomUUID())
            .description(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .taskRef(UUID.randomUUID().toString());
    }
}
