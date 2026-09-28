package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.AlertSourceAsserts.*;
import static cm.gimac.des.monitoring.domain.AlertSourceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertSourceMapperTest {

    private AlertSourceMapper alertSourceMapper;

    @BeforeEach
    void setUp() {
        alertSourceMapper = new AlertSourceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAlertSourceSample1();
        var actual = alertSourceMapper.toEntity(alertSourceMapper.toDto(expected));
        assertAlertSourceAllPropertiesEquals(expected, actual);
    }
}
