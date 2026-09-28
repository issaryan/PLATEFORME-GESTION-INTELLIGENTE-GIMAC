package cm.gimac.des.collaboration.domain;

import java.util.UUID;

public class RoomMemberTestSamples {

    public static RoomMember getRoomMemberSample1() {
        return new RoomMember()
            .id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .userId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"))
            .replacesUserId(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa"));
    }

    public static RoomMember getRoomMemberSample2() {
        return new RoomMember()
            .id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .userId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"))
            .replacesUserId(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367"));
    }

    public static RoomMember getRoomMemberRandomSampleGenerator() {
        return new RoomMember().id(UUID.randomUUID()).userId(UUID.randomUUID()).replacesUserId(UUID.randomUUID());
    }
}
