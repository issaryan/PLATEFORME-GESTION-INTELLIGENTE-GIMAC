package cm.gimac.des.collaboration.service.mapper;

import static cm.gimac.des.collaboration.domain.RoomRetentionPolicyAsserts.*;
import static cm.gimac.des.collaboration.domain.RoomRetentionPolicyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomRetentionPolicyMapperTest {

    private RoomRetentionPolicyMapper roomRetentionPolicyMapper;

    @BeforeEach
    void setUp() {
        roomRetentionPolicyMapper = new RoomRetentionPolicyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoomRetentionPolicySample1();
        var actual = roomRetentionPolicyMapper.toEntity(roomRetentionPolicyMapper.toDto(expected));
        assertRoomRetentionPolicyAllPropertiesEquals(expected, actual);
    }
}
