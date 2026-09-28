package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomRetentionPolicyTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static RoomRetentionPolicy getRoomRetentionPolicySample1() {
        return new RoomRetentionPolicy().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).archiveRetentionYears(1);
    }

    public static RoomRetentionPolicy getRoomRetentionPolicySample2() {
        return new RoomRetentionPolicy().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).archiveRetentionYears(2);
    }

    public static RoomRetentionPolicy getRoomRetentionPolicyRandomSampleGenerator() {
        return new RoomRetentionPolicy().id(UUID.randomUUID()).archiveRetentionYears(intCount.incrementAndGet());
    }
}
