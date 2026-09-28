package cm.gimac.des.participant.domain;

import java.util.UUID;

public class SubscribedServiceTestSamples {

    public static SubscribedService getSubscribedServiceSample1() {
        return new SubscribedService().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).serviceCode("serviceCode1");
    }

    public static SubscribedService getSubscribedServiceSample2() {
        return new SubscribedService().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).serviceCode("serviceCode2");
    }

    public static SubscribedService getSubscribedServiceRandomSampleGenerator() {
        return new SubscribedService().id(UUID.randomUUID()).serviceCode(UUID.randomUUID().toString());
    }
}
