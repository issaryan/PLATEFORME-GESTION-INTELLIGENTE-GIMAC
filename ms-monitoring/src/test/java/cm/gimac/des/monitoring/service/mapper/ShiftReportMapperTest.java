package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.ShiftReportAsserts.*;
import static cm.gimac.des.monitoring.domain.ShiftReportTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShiftReportMapperTest {

    private ShiftReportMapper shiftReportMapper;

    @BeforeEach
    void setUp() {
        shiftReportMapper = new ShiftReportMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getShiftReportSample1();
        var actual = shiftReportMapper.toEntity(shiftReportMapper.toDto(expected));
        assertShiftReportAllPropertiesEquals(expected, actual);
    }
}
