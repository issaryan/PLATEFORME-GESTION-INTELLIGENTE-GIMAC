package cm.gimac.des.collaboration.domain;

import java.util.UUID;

public class ConferenceParticipantTestSamples {

    public static ConferenceParticipant getConferenceParticipantSample1() {
        return new ConferenceParticipant()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .terminalId("terminalId1");
    }

    public static ConferenceParticipant getConferenceParticipantSample2() {
        return new ConferenceParticipant()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .terminalId("terminalId2");
    }

    public static ConferenceParticipant getConferenceParticipantRandomSampleGenerator() {
        return new ConferenceParticipant().id(UUID.randomUUID()).userId(UUID.randomUUID()).terminalId(UUID.randomUUID().toString());
    }
}
