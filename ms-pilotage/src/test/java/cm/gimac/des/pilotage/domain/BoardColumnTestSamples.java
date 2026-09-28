package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BoardColumnTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static BoardColumn getBoardColumnSample1() {
        return new BoardColumn()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .code("code1")
            .label("label1")
            .position(1)
            .wipLimit(1);
    }

    public static BoardColumn getBoardColumnSample2() {
        return new BoardColumn()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .code("code2")
            .label("label2")
            .position(2)
            .wipLimit(2);
    }

    public static BoardColumn getBoardColumnRandomSampleGenerator() {
        return new BoardColumn()
            .id(UUID.randomUUID())
            .code(UUID.randomUUID().toString())
            .label(UUID.randomUUID().toString())
            .position(intCount.incrementAndGet())
            .wipLimit(intCount.incrementAndGet());
    }
}
