package cm.gimac.des.notification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryTraceTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static DeliveryTrace getDeliveryTraceSample1() {
        return new DeliveryTrace()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .attempts(1)
            .lastError("lastError1")
            .providerMessageId("providerMessageId1");
    }

    public static DeliveryTrace getDeliveryTraceSample2() {
        return new DeliveryTrace()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .attempts(2)
            .lastError("lastError2")
            .providerMessageId("providerMessageId2");
    }

    public static DeliveryTrace getDeliveryTraceRandomSampleGenerator() {
        return new DeliveryTrace()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .attempts(intCount.incrementAndGet())
            .lastError(UUID.randomUUID().toString())
            .providerMessageId(UUID.randomUUID().toString());
    }
}
