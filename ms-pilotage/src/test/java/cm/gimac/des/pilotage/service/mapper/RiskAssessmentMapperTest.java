package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.RiskAssessmentAsserts.*;
import static cm.gimac.des.pilotage.domain.RiskAssessmentTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskAssessmentMapperTest {

    private RiskAssessmentMapper riskAssessmentMapper;

    @BeforeEach
    void setUp() {
        riskAssessmentMapper = new RiskAssessmentMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRiskAssessmentSample1();
        var actual = riskAssessmentMapper.toEntity(riskAssessmentMapper.toDto(expected));
        assertRiskAssessmentAllPropertiesEquals(expected, actual);
    }
}
