package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AgingNoticeTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static AgingNotice getAgingNoticeSample1() {
        return new AgingNotice().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).ageInDays(1);
    }

    public static AgingNotice getAgingNoticeSample2() {
        return new AgingNotice().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).ageInDays(2);
    }

    public static AgingNotice getAgingNoticeRandomSampleGenerator() {
        return new AgingNotice().id(UUID.randomUUID()).ageInDays(intCount.incrementAndGet());
    }
}
