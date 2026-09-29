package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ReferentialMigrationTestSamples {

    public static ReferentialMigration getReferentialMigrationSample1() {
        return new ReferentialMigration()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .versionSourceId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .versionCibleId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .decideurId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ReferentialMigration getReferentialMigrationSample2() {
        return new ReferentialMigration()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .versionSourceId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .versionCibleId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .decideurId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ReferentialMigration getReferentialMigrationRandomSampleGenerator() {
        return new ReferentialMigration()
            .id(UUID.randomUUID())
            .versionSourceId(UUID.randomUUID())
            .versionCibleId(UUID.randomUUID())
            .decideurId(UUID.randomUUID());
    }
}
