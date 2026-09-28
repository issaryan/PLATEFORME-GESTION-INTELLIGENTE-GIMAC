package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.ShiftLogEntryAsserts.*;
import static cm.gimac.des.monitoring.domain.ShiftLogEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShiftLogEntryMapperTest {

    private ShiftLogEntryMapper shiftLogEntryMapper;

    @BeforeEach
    void setUp() {
        shiftLogEntryMapper = new ShiftLogEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getShiftLogEntrySample1();
        var actual = shiftLogEntryMapper.toEntity(shiftLogEntryMapper.toDto(expected));
        assertShiftLogEntryAllPropertiesEquals(expected, actual);
    }
}
