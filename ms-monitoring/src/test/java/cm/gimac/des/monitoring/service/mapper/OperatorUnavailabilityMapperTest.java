package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.OperatorUnavailabilityAsserts.*;
import static cm.gimac.des.monitoring.domain.OperatorUnavailabilityTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperatorUnavailabilityMapperTest {

    private OperatorUnavailabilityMapper operatorUnavailabilityMapper;

    @BeforeEach
    void setUp() {
        operatorUnavailabilityMapper = new OperatorUnavailabilityMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getOperatorUnavailabilitySample1();
        var actual = operatorUnavailabilityMapper.toEntity(operatorUnavailabilityMapper.toDto(expected));
        assertOperatorUnavailabilityAllPropertiesEquals(expected, actual);
    }
}
