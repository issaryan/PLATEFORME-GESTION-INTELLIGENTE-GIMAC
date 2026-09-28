package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.DimensionAsserts.*;
import static cm.gimac.des.reporting.domain.DimensionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DimensionMapperTest {

    private DimensionMapper dimensionMapper;

    @BeforeEach
    void setUp() {
        dimensionMapper = new DimensionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDimensionSample1();
        var actual = dimensionMapper.toEntity(dimensionMapper.toDto(expected));
        assertDimensionAllPropertiesEquals(expected, actual);
    }
}
