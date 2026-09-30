package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ConferenceTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Conference getConferenceSample1() {
        return new Conference().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).sfuRoom("sfuRoom1").keyEpoch(1);
    }

    public static Conference getConferenceSample2() {
        return new Conference().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).sfuRoom("sfuRoom2").keyEpoch(2);
    }

    public static Conference getConferenceRandomSampleGenerator() {
        return new Conference().id(UUID.randomUUID()).sfuRoom(UUID.randomUUID().toString()).keyEpoch(intCount.incrementAndGet());
    }
}
