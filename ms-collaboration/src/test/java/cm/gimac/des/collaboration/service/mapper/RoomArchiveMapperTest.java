package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.RoomArchiveAsserts.*;
import static cm.gimac.des.collaboration.domain.RoomArchiveTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomArchiveMapperTest {

    private RoomArchiveMapper roomArchiveMapper;

    @BeforeEach
    void setUp() {
        roomArchiveMapper = new RoomArchiveMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomArchiveSample1();
        var actual = roomArchiveMapper.toEntity(roomArchiveMapper.toDto(expected));
        assertRoomArchiveAllPropertiesEquals(expected, actual);
    }
}
