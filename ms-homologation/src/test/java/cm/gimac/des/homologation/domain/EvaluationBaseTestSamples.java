package cm.gimac.des.homologation.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EvaluationBaseTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static EvaluationBase getEvaluationBaseSample1() {
        return new EvaluationBase().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).empreinte("empreinte1").cycle(1);
    }

    public static EvaluationBase getEvaluationBaseSample2() {
        return new EvaluationBase().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).empreinte("empreinte2").cycle(2);
    }

    public static EvaluationBase getEvaluationBaseRandomSampleGenerator() {
        return new EvaluationBase().id(UUID.randomUUID()).empreinte(UUID.randomUUID().toString()).cycle(intCount.incrementAndGet());
    }
}
