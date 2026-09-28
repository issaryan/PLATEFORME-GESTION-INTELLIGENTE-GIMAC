package cm.gimac.des.notification.service.mapper;

import static cm.gimac.des.notification.domain.AcknowledgementPolicyAsserts.*;
import static cm.gimac.des.notification.domain.AcknowledgementPolicyTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcknowledgementPolicyMapperTest {

    private AcknowledgementPolicyMapper acknowledgementPolicyMapper;

    @BeforeEach
    void setUp() {
        acknowledgementPolicyMapper = new AcknowledgementPolicyMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAcknowledgementPolicySample1();
        var actual = acknowledgementPolicyMapper.toEntity(acknowledgementPolicyMapper.toDto(expected));
        assertAcknowledgementPolicyAllPropertiesEquals(expected, actual);
    }
}
