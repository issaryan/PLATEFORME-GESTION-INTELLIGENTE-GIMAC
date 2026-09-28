package cm.gimac.des.pilotage.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ActionSheetTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static ActionSheet getActionSheetSample1() {
        return new ActionSheet()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .originReference("originReference1")
            .originService("originService1")
            .objective("objective1")
            .causeAnalysis("causeAnalysis1")
            .ownerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .result("result1")
            .cycle(1)
            .procedureDocumentRef(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static ActionSheet getActionSheetSample2() {
        return new ActionSheet()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .originReference("originReference2")
            .originService("originService2")
            .objective("objective2")
            .causeAnalysis("causeAnalysis2")
            .ownerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .result("result2")
            .cycle(2)
            .procedureDocumentRef(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static ActionSheet getActionSheetRandomSampleGenerator() {
        return new ActionSheet()
            .id(UUID.randomUUID())
            .originReference(UUID.randomUUID().toString())
            .originService(UUID.randomUUID().toString())
            .objective(UUID.randomUUID().toString())
            .causeAnalysis(UUID.randomUUID().toString())
            .ownerId(UUID.randomUUID())
            .result(UUID.randomUUID().toString())
            .cycle(intCount.incrementAndGet())
            .procedureDocumentRef(UUID.randomUUID());
    }
}
