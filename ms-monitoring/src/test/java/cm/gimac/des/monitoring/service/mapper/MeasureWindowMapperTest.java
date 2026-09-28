package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.MeasureWindowAsserts.*;
import static cm.gimac.des.monitoring.domain.MeasureWindowTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeasureWindowMapperTest {

    private MeasureWindowMapper measureWindowMapper;

    @BeforeEach
    void setUp() {
        measureWindowMapper = new MeasureWindowMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getMeasureWindowSample1();
        var actual = measureWindowMapper.toEntity(measureWindowMapper.toDto(expected));
        assertMeasureWindowAllPropertiesEquals(expected, actual);
    }
}
