package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class MeetingTestSamples {

    public static Meeting getMeetingSample1() {
        return new Meeting()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .instance("instance1")
            .minutesRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .minutesDigest("minutesDigest1");
    }

    public static Meeting getMeetingSample2() {
        return new Meeting()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .instance("instance2")
            .minutesRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .minutesDigest("minutesDigest2");
    }

    public static Meeting getMeetingRandomSampleGenerator() {
        return new Meeting()
            .id(UUID.randomUUID())
            .instance(UUID.randomUUID().toString())
            .minutesRef(UUID.randomUUID())
            .minutesDigest(UUID.randomUUID().toString());
    }
}
