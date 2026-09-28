package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.DeadlineAsserts.*;
import static cm.gimac.des.participant.domain.DeadlineTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeadlineMapperTest {

    private DeadlineMapper deadlineMapper;

    @BeforeEach
    void setUp() {
        deadlineMapper = new DeadlineMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDeadlineSample1();
        var actual = deadlineMapper.toEntity(deadlineMapper.toDto(expected));
        assertDeadlineAllPropertiesEquals(expected, actual);
    }
}
