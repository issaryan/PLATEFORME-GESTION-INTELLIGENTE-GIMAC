package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.DecisionAsserts.*;
import static cm.gimac.des.pilotage.domain.DecisionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecisionMapperTest {

    private DecisionMapper decisionMapper;

    @BeforeEach
    void setUp() {
        decisionMapper = new DecisionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getDecisionSample1();
        var actual = decisionMapper.toEntity(decisionMapper.toDto(expected));
        assertDecisionAllPropertiesEquals(expected, actual);
    }
}
