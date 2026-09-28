package cm.gimac.des.incident.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CrisisMemberTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static CrisisMember getCrisisMemberSample1() {
        return new CrisisMember()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .role("role1")
            .weight(1)
            .substituteFor(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static CrisisMember getCrisisMemberSample2() {
        return new CrisisMember()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .role("role2")
            .weight(2)
            .substituteFor(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static CrisisMember getCrisisMemberRandomSampleGenerator() {
        return new CrisisMember()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .role(UUID.randomUUID().toString())
            .weight(intCount.incrementAndGet())
            .substituteFor(UUID.randomUUID());
    }
}
