package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.IndicatorDefinitionAsserts.*;
import static cm.gimac.des.reporting.domain.IndicatorDefinitionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndicatorDefinitionMapperTest {

    private IndicatorDefinitionMapper indicatorDefinitionMapper;

    @BeforeEach
    void setUp() {
        indicatorDefinitionMapper = new IndicatorDefinitionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIndicatorDefinitionSample1();
        var actual = indicatorDefinitionMapper.toEntity(indicatorDefinitionMapper.toDto(expected));
        assertIndicatorDefinitionAllPropertiesEquals(expected, actual);
    }
}
