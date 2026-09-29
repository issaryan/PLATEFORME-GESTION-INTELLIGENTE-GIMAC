package cm.gimac.des.certification.domain;

import java.util.UUID;

public class TimestampTokenTestSamples {

    public static TimestampToken getTimestampTokenSample1() {
        return new TimestampToken()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .objetType("objetType1")
            .objetId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .empreinte("empreinte1")
            .nonce("nonce1")
            .autoriteChaine("autoriteChaine1");
    }

    public static TimestampToken getTimestampTokenSample2() {
        return new TimestampToken()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .objetType("objetType2")
            .objetId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .empreinte("empreinte2")
            .nonce("nonce2")
            .autoriteChaine("autoriteChaine2");
    }

    public static TimestampToken getTimestampTokenRandomSampleGenerator() {
        return new TimestampToken()
            .id(UUID.randomUUID())
            .objetType(UUID.randomUUID().toString())
            .objetId(UUID.randomUUID())
            .empreinte(UUID.randomUUID().toString())
            .nonce(UUID.randomUUID().toString())
            .autoriteChaine(UUID.randomUUID().toString());
    }
}
