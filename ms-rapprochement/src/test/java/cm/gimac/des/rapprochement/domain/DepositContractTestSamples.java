package cm.gimac.des.rapprochement.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DepositContractTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static DepositContract getDepositContractSample1() {
        return new DepositContract()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .institutionId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .requiredColumns("requiredColumns1")
            .encoding("encoding1")
            .separator("separator1")
            .maxSizeBytes(1L);
    }

    public static DepositContract getDepositContractSample2() {
        return new DepositContract()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .institutionId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .requiredColumns("requiredColumns2")
            .encoding("encoding2")
            .separator("separator2")
            .maxSizeBytes(2L);
    }

    public static DepositContract getDepositContractRandomSampleGenerator() {
        return new DepositContract()
            .id(UUID.randomUUID())
            .institutionId(UUID.randomUUID())
            .requiredColumns(UUID.randomUUID().toString())
            .encoding(UUID.randomUUID().toString())
            .separator(UUID.randomUUID().toString())
            .maxSizeBytes(longCount.incrementAndGet());
    }
}
