package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class StatementLineTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static StatementLine getStatementLineSample1() {
        return new StatementLine()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .lineNumber(1)
            .transactionRef("transactionRef1")
            .authorizationRef("authorizationRef1")
            .participantCode("participantCode1")
            .lineStatus("lineStatus1")
            .currency("currency1");
    }

    public static StatementLine getStatementLineSample2() {
        return new StatementLine()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .lineNumber(2)
            .transactionRef("transactionRef2")
            .authorizationRef("authorizationRef2")
            .participantCode("participantCode2")
            .lineStatus("lineStatus2")
            .currency("currency2");
    }

    public static StatementLine getStatementLineRandomSampleGenerator() {
        return new StatementLine()
            .id(UUID.randomUUID())
            .lineNumber(intCount.incrementAndGet())
            .transactionRef(UUID.randomUUID().toString())
            .authorizationRef(UUID.randomUUID().toString())
            .participantCode(UUID.randomUUID().toString())
            .lineStatus(UUID.randomUUID().toString())
            .currency(UUID.randomUUID().toString());
    }
}
