package cm.gimac.des.monitoring.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class OnCallRosterTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static OnCallRoster getOnCallRosterSample1() {
        return new OnCallRoster()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .isoWeek("isoWeek1")
            .revision(1)
            .generatedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .validatedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static OnCallRoster getOnCallRosterSample2() {
        return new OnCallRoster()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .isoWeek("isoWeek2")
            .revision(2)
            .generatedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .validatedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static OnCallRoster getOnCallRosterRandomSampleGenerator() {
        return new OnCallRoster()
            .id(UUID.randomUUID())
            .isoWeek(UUID.randomUUID().toString())
            .revision(intCount.incrementAndGet())
            .generatedBy(UUID.randomUUID())
            .validatedBy(UUID.randomUUID());
    }
}
