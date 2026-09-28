package cm.gimac.des.collaboration.domain;

import java.util.UUID;

public class RecordingConsentTestSamples {

    public static RecordingConsent getRecordingConsentSample1() {
        return new RecordingConsent()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static RecordingConsent getRecordingConsentSample2() {
        return new RecordingConsent()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static RecordingConsent getRecordingConsentRandomSampleGenerator() {
        return new RecordingConsent().id(UUID.randomUUID()).userId(UUID.randomUUID());
    }
}
