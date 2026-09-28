package cm.gimac.des.monitoring.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class OnCallAssignmentTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OnCallAssignment getOnCallAssignmentSample1() {
        return new OnCallAssignment()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .level(1)
            .operatorId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static OnCallAssignment getOnCallAssignmentSample2() {
        return new OnCallAssignment()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .level(2)
            .operatorId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static OnCallAssignment getOnCallAssignmentRandomSampleGenerator() {
        return new OnCallAssignment().id(UUID.randomUUID()).level(intCount.incrementAndGet()).operatorId(UUID.randomUUID());
    }
}
