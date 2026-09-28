package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.IndicatorValueAsserts.*;
import static cm.gimac.des.reporting.domain.IndicatorValueTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicatorValueMapperTest {

    private IndicatorValueMapper indicatorValueMapper;

    @BeforeEach
    void setUp() {
        indicatorValueMapper = new IndicatorValueMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIndicatorValueSample1();
        var actual = indicatorValueMapper.toEntity(indicatorValueMapper.toDto(expected));
        assertIndicatorValueAllPropertiesEquals(expected, actual);
    }
}
