package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.RiskAsserts.*;
import static cm.gimac.des.pilotage.domain.RiskTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskMapperTest {

    private RiskMapper riskMapper;

    @BeforeEach
    void setUp() {
        riskMapper = new RiskMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRiskSample1();
        var actual = riskMapper.toEntity(riskMapper.toDto(expected));
        assertRiskAllPropertiesEquals(expected, actual);
    }
}
