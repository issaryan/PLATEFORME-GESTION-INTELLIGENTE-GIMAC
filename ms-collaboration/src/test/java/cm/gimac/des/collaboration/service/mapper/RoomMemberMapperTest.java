package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.RoomMemberAsserts.*;
import static cm.gimac.des.collaboration.domain.RoomMemberTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomMemberMapperTest {

    private RoomMemberMapper roomMemberMapper;

    @BeforeEach
    void setUp() {
        roomMemberMapper = new RoomMemberMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomMemberSample1();
        var actual = roomMemberMapper.toEntity(roomMemberMapper.toDto(expected));
        assertRoomMemberAllPropertiesEquals(expected, actual);
    }
}
