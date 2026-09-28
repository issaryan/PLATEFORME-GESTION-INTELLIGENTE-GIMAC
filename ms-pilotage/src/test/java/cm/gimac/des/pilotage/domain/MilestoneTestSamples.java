package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class MilestoneTestSamples {

    public static Milestone getMilestoneSample1() {
        return new Milestone().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).label("label1");
    }

    public static Milestone getMilestoneSample2() {
        return new Milestone().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).label("label2");
    }

    public static Milestone getMilestoneRandomSampleGenerator() {
        return new Milestone().id(UUID.randomUUID()).label(UUID.randomUUID().toString());
    }
}
