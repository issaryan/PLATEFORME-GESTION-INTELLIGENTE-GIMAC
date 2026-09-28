package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.StatusTransitionAsserts.*;
import static cm.gimac.des.participant.domain.StatusTransitionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusTransitionMapperTest {

    private StatusTransitionMapper statusTransitionMapper;

    @BeforeEach
    void setUp() {
        statusTransitionMapper = new StatusTransitionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getStatusTransitionSample1();
        var actual = statusTransitionMapper.toEntity(statusTransitionMapper.toDto(expected));
        assertStatusTransitionAllPropertiesEquals(expected, actual);
    }
}
