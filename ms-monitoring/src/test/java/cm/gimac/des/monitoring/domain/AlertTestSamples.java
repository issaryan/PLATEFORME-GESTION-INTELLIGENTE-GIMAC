package cm.gimac.des.monitoring.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AlertTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Alert getAlertSample1() {
        return new Alert()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reference("reference1")
            .source("source1")
            .rule("rule1")
            .fingerprint("fingerprint1")
            .occurrences(1)
            .acknowledgedBy(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .ackComment("ackComment1")
            .ticketId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static Alert getAlertSample2() {
        return new Alert()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reference("reference2")
            .source("source2")
            .rule("rule2")
            .fingerprint("fingerprint2")
            .occurrences(2)
            .acknowledgedBy(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .ackComment("ackComment2")
            .ticketId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static Alert getAlertRandomSampleGenerator() {
        return new Alert()
            .id(UUID.randomUUID())
            .reference(UUID.randomUUID().toString())
            .source(UUID.randomUUID().toString())
            .rule(UUID.randomUUID().toString())
            .fingerprint(UUID.randomUUID().toString())
            .occurrences(intCount.incrementAndGet())
            .acknowledgedBy(UUID.randomUUID())
            .ackComment(UUID.randomUUID().toString())
            .ticketId(UUID.randomUUID());
    }
}
