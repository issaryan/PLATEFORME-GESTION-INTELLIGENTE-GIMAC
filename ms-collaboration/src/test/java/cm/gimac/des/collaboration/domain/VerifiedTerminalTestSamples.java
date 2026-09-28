package cm.gimac.des.collaboration.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class VerifiedTerminalTestSamples {

    private static final Random random = new Random();
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static VerifiedTerminal getVerifiedTerminalSample1() {
        return new VerifiedTerminal()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .terminalId("terminalId1")
            .identityKey("identityKey1")
            .agreementKey("agreementKey1")
            .signedPreKey("signedPreKey1")
            .signedPreKeySignature("signedPreKeySignature1")
            .oneTimePreKeys(1)
            .crossSignature("crossSignature1");
    }

    public static VerifiedTerminal getVerifiedTerminalSample2() {
        return new VerifiedTerminal()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .terminalId("terminalId2")
            .identityKey("identityKey2")
            .agreementKey("agreementKey2")
            .signedPreKey("signedPreKey2")
            .signedPreKeySignature("signedPreKeySignature2")
            .oneTimePreKeys(2)
            .crossSignature("crossSignature2");
    }

    public static VerifiedTerminal getVerifiedTerminalRandomSampleGenerator() {
        return new VerifiedTerminal()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .terminalId(UUID.randomUUID().toString())
            .identityKey(UUID.randomUUID().toString())
            .agreementKey(UUID.randomUUID().toString())
            .signedPreKey(UUID.randomUUID().toString())
            .signedPreKeySignature(UUID.randomUUID().toString())
            .oneTimePreKeys(intCount.incrementAndGet())
            .crossSignature(UUID.randomUUID().toString());
    }
}
