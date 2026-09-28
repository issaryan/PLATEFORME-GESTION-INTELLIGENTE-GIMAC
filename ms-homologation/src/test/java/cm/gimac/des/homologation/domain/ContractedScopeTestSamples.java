package cm.gimac.des.homologation.domain;

import java.util.UUID;

public class ContractedScopeTestSamples {

    public static ContractedScope getContractedScopeSample1() {
        return new ContractedScope().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).serviceCode("serviceCode1");
    }

    public static ContractedScope getContractedScopeSample2() {
        return new ContractedScope().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).serviceCode("serviceCode2");
    }

    public static ContractedScope getContractedScopeRandomSampleGenerator() {
        return new ContractedScope().id(UUID.randomUUID()).serviceCode(UUID.randomUUID().toString());
    }
}
