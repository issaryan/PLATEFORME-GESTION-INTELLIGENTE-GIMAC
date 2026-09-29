package cm.gimac.des.certification.domain;

import java.util.UUID;

public class GoLiveDossierTestSamples {

    public static GoLiveDossier getGoLiveDossierSample1() {
        return new GoLiveDossier()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .liaisonId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .referenceFicheChangement("referenceFicheChangement1");
    }

    public static GoLiveDossier getGoLiveDossierSample2() {
        return new GoLiveDossier()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .liaisonId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .referenceFicheChangement("referenceFicheChangement2");
    }

    public static GoLiveDossier getGoLiveDossierRandomSampleGenerator() {
        return new GoLiveDossier()
            .id(UUID.randomUUID())
            .liaisonId(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .referenceFicheChangement(UUID.randomUUID().toString());
    }
}
