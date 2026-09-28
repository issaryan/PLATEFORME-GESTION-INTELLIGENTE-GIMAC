package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.PeriodicReportAsserts.*;
import static cm.gimac.des.reporting.domain.PeriodicReportTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeriodicReportMapperTest {

    private PeriodicReportMapper periodicReportMapper;

    @BeforeEach
    void setUp() {
        periodicReportMapper = new PeriodicReportMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPeriodicReportSample1();
        var actual = periodicReportMapper.toEntity(periodicReportMapper.toDto(expected));
        assertPeriodicReportAllPropertiesEquals(expected, actual);
    }
}
