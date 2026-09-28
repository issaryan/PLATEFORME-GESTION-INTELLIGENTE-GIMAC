package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class CampaignSyncTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static CampaignSync getCampaignSyncSample1() {
        return new CampaignSync().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).consecutiveFailures(1);
    }

    public static CampaignSync getCampaignSyncSample2() {
        return new CampaignSync().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).consecutiveFailures(2);
    }

    public static CampaignSync getCampaignSyncRandomSampleGenerator() {
        return new CampaignSync().id(UUID.randomUUID()).consecutiveFailures(intCount.incrementAndGet());
    }
}
