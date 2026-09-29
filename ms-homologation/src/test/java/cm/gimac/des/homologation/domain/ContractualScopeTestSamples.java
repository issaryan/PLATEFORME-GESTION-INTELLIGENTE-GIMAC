package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ContractualScopeTestSamples {

    public static ContractualScope getContractualScopeSample1() {
        return new ContractualScope().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).sourceEventId("sourceEventId1");
    }

    public static ContractualScope getContractualScopeSample2() {
        return new ContractualScope().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).sourceEventId("sourceEventId2");
    }

    public static ContractualScope getContractualScopeRandomSampleGenerator() {
        return new ContractualScope().id(UUID.randomUUID()).sourceEventId(UUID.randomUUID().toString());
    }
}
