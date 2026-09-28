package cm.gimac.des.participant.domain;

import java.util.UUID;

public class ConventionTestSamples {

    public static Convention getConventionSample1() {
        return new Convention()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .documentId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .documentSha256("documentSha2561");
    }

    public static Convention getConventionSample2() {
        return new Convention()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .documentId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .documentSha256("documentSha2562");
    }

    public static Convention getConventionRandomSampleGenerator() {
        return new Convention()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .documentId(UUID.randomUUID())
            .documentSha256(UUID.randomUUID().toString());
    }
}
