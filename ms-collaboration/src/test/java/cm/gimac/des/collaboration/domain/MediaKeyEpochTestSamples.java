package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaKeyEpochTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static MediaKeyEpoch getMediaKeyEpochSample1() {
        return new MediaKeyEpoch().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).epoch(1);
    }

    public static MediaKeyEpoch getMediaKeyEpochSample2() {
        return new MediaKeyEpoch().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).epoch(2);
    }

    public static MediaKeyEpoch getMediaKeyEpochRandomSampleGenerator() {
        return new MediaKeyEpoch().id(UUID.randomUUID()).epoch(intCount.incrementAndGet());
    }
}
