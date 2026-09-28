package cm.gimac.des.monitoring.service.mapper;

import static cm.gimac.des.monitoring.domain.AlertSeverityPolicyAsserts.*;
import static cm.gimac.des.monitoring.domain.AlertSeverityPolicyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertSeverityPolicyMapperTest {

    private AlertSeverityPolicyMapper alertSeverityPolicyMapper;

    @BeforeEach
    void setUp() {
        alertSeverityPolicyMapper = new AlertSeverityPolicyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAlertSeverityPolicySample1();
        var actual = alertSeverityPolicyMapper.toEntity(alertSeverityPolicyMapper.toDto(expected));
        assertAlertSeverityPolicyAllPropertiesEquals(expected, actual);
    }
}
