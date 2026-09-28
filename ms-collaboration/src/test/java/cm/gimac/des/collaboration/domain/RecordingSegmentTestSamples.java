package cm.gimac.des.collaboration.domain;

import java.util.UUID;

public class RecordingSegmentTestSamples {

    public static RecordingSegment getRecordingSegmentSample1() {
        return new RecordingSegment()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .egressReference("egressReference1")
            .consentingUserIds("consentingUserIds1");
    }

    public static RecordingSegment getRecordingSegmentSample2() {
        return new RecordingSegment()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .egressReference("egressReference2")
            .consentingUserIds("consentingUserIds2");
    }

    public static RecordingSegment getRecordingSegmentRandomSampleGenerator() {
        return new RecordingSegment()
            .id(UUID.randomUUID())
            .egressReference(UUID.randomUUID().toString())
            .consentingUserIds(UUID.randomUUID().toString());
    }
}
