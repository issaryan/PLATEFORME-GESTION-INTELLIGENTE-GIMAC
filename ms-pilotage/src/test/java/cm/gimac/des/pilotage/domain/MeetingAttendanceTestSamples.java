package cm.gimac.des.pilotage.domain;

import java.util.UUID;

public class MeetingAttendanceTestSamples {

    public static MeetingAttendance getMeetingAttendanceSample1() {
        return new MeetingAttendance()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .participantId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static MeetingAttendance getMeetingAttendanceSample2() {
        return new MeetingAttendance()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .participantId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static MeetingAttendance getMeetingAttendanceRandomSampleGenerator() {
        return new MeetingAttendance().id(UUID.randomUUID()).participantId(UUID.randomUUID());
    }
}
