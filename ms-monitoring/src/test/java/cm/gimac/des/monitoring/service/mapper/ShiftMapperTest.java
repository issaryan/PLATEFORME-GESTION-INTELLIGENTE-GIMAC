package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.ShiftAsserts.*;
import static cm.gimac.des.monitoring.domain.ShiftTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShiftMapperTest {

    private ShiftMapper shiftMapper;

    @BeforeEach
    void setUp() {
        shiftMapper = new ShiftMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getShiftSample1();
        var actual = shiftMapper.toEntity(shiftMapper.toDto(expected));
        assertShiftAllPropertiesEquals(expected, actual);
    }
}
