package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Project getProjectSample1() {
        return new Project()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .title("title1")
            .scope("scope1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .progress(1);
    }

    public static Project getProjectSample2() {
        return new Project()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .title("title2")
            .scope("scope2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .progress(2);
    }

    public static Project getProjectRandomSampleGenerator() {
        return new Project()
            .id(UUID.randomUUID())
            .title(UUID.randomUUID().toString())
            .scope(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .progress(intCount.incrementAndGet());
    }
}
