package cm.gimac.des.incident.domain;

import java.util.UUID;

public class ServiceCommitmentTestSamples {

    public static ServiceCommitment getServiceCommitmentSample1() {
        return new ServiceCommitment().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ServiceCommitment getServiceCommitmentSample2() {
        return new ServiceCommitment().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ServiceCommitment getServiceCommitmentRandomSampleGenerator() {
        return new ServiceCommitment().id(UUID.randomUUID());
    }
}
