package cm.gimac.des.reporting.domain;

import java.util.UUID;

public class ProcessedEventTestSamples {

    public static ProcessedEvent getProcessedEventSample1() {
        return new ProcessedEvent()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .eventId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .subject("subject1");
    }

    public static ProcessedEvent getProcessedEventSample2() {
        return new ProcessedEvent()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .eventId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .subject("subject2");
    }

    public static ProcessedEvent getProcessedEventRandomSampleGenerator() {
        return new ProcessedEvent().id(UUID.randomUUID()).eventId(UUID.randomUUID()).subject(UUID.randomUUID().toString());
    }
}
