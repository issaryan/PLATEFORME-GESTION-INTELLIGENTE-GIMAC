package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class BoardTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Board getBoardSample1() {
        return new Board()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .serviceCode("serviceCode1")
            .name("name1")
            .version(1L);
    }

    public static Board getBoardSample2() {
        return new Board()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .serviceCode("serviceCode2")
            .name("name2")
            .version(2L);
    }

    public static Board getBoardRandomSampleGenerator() {
        return new Board()
            .id(UUID.randomUUID())
            .serviceCode(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .version(longCount.incrementAndGet());
    }
}
