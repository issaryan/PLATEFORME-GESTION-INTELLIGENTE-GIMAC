package cm.gimac.des.certification.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class QuitusTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Quitus getQuitusSample1() {
        return new Quitus()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .signerId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .reason("reason1")
            .basisDigest("basisDigest1")
            .decisionDigest("decisionDigest1")
            .credentialId("credentialId1")
            .signCount(1L);
    }

    public static Quitus getQuitusSample2() {
        return new Quitus()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .signerId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .reason("reason2")
            .basisDigest("basisDigest2")
            .decisionDigest("decisionDigest2")
            .credentialId("credentialId2")
            .signCount(2L);
    }

    public static Quitus getQuitusRandomSampleGenerator() {
        return new Quitus()
            .id(UUID.randomUUID())
            .signerId(UUID.randomUUID())
            .reason(UUID.randomUUID().toString())
            .basisDigest(UUID.randomUUID().toString())
            .decisionDigest(UUID.randomUUID().toString())
            .credentialId(UUID.randomUUID().toString())
            .signCount(longCount.incrementAndGet());
    }
}
