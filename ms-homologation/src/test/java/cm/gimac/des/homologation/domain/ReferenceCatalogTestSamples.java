package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ReferenceCatalogTestSamples {

    public static ReferenceCatalog getReferenceCatalogSample1() {
        return new ReferenceCatalog()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .version("version1")
            .authorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .approverId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ReferenceCatalog getReferenceCatalogSample2() {
        return new ReferenceCatalog()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .version("version2")
            .authorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .approverId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ReferenceCatalog getReferenceCatalogRandomSampleGenerator() {
        return new ReferenceCatalog()
            .id(UUID.randomUUID())
            .version(UUID.randomUUID().toString())
            .authorId(UUID.randomUUID())
            .approverId(UUID.randomUUID());
    }
}
