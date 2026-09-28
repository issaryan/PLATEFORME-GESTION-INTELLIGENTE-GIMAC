package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.PeriodRootAsserts.*;
import static cm.gimac.des.audit.domain.PeriodRootTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeriodRootMapperTest {

    private PeriodRootMapper periodRootMapper;

    @BeforeEach
    void setUp() {
        periodRootMapper = new PeriodRootMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPeriodRootSample1();
        var actual = periodRootMapper.toEntity(periodRootMapper.toDto(expected));
        assertPeriodRootAllPropertiesEquals(expected, actual);
    }
}
