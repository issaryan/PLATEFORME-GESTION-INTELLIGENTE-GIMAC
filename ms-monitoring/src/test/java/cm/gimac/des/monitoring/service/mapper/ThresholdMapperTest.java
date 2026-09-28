package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.ThresholdAsserts.*;
import static cm.gimac.des.monitoring.domain.ThresholdTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThresholdMapperTest {

    private ThresholdMapper thresholdMapper;

    @BeforeEach
    void setUp() {
        thresholdMapper = new ThresholdMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getThresholdSample1();
        var actual = thresholdMapper.toEntity(thresholdMapper.toDto(expected));
        assertThresholdAllPropertiesEquals(expected, actual);
    }
}
