package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.AlertObservationAsserts.*;
import static cm.gimac.des.incident.domain.AlertObservationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertObservationMapperTest {

    private AlertObservationMapper alertObservationMapper;

    @BeforeEach
    void setUp() {
        alertObservationMapper = new AlertObservationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAlertObservationSample1();
        var actual = alertObservationMapper.toEntity(alertObservationMapper.toDto(expected));
        assertAlertObservationAllPropertiesEquals(expected, actual);
    }
}
