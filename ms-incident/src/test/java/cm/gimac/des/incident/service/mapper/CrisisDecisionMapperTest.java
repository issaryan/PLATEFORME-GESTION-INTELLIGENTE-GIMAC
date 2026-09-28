package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.CrisisDecisionAsserts.*;
import static cm.gimac.des.incident.domain.CrisisDecisionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrisisDecisionMapperTest {

    private CrisisDecisionMapper crisisDecisionMapper;

    @BeforeEach
    void setUp() {
        crisisDecisionMapper = new CrisisDecisionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCrisisDecisionSample1();
        var actual = crisisDecisionMapper.toEntity(crisisDecisionMapper.toDto(expected));
        assertCrisisDecisionAllPropertiesEquals(expected, actual);
    }
}
