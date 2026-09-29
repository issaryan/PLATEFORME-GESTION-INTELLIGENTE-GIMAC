package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ReferenceOperationTestSamples {

    public static ReferenceOperation getReferenceOperationSample1() {
        return new ReferenceOperation()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .libelle("libelle1")
            .phase("phase1")
            .serviceCode("serviceCode1")
            .idScenario("idScenario1")
            .fonctionResponsable("fonctionResponsable1");
    }

    public static ReferenceOperation getReferenceOperationSample2() {
        return new ReferenceOperation()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .libelle("libelle2")
            .phase("phase2")
            .serviceCode("serviceCode2")
            .idScenario("idScenario2")
            .fonctionResponsable("fonctionResponsable2");
    }

    public static ReferenceOperation getReferenceOperationRandomSampleGenerator() {
        return new ReferenceOperation()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .libelle(UUID.randomUUID().toString())
            .phase(UUID.randomUUID().toString())
            .serviceCode(UUID.randomUUID().toString())
            .idScenario(UUID.randomUUID().toString())
            .fonctionResponsable(UUID.randomUUID().toString());
    }
}
