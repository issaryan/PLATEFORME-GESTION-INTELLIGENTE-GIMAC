package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.AcceptanceCriterionAsserts.*;
import static cm.gimac.des.pilotage.domain.AcceptanceCriterionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcceptanceCriterionMapperTest {

    private AcceptanceCriterionMapper acceptanceCriterionMapper;

    @BeforeEach
    void setUp() {
        acceptanceCriterionMapper = new AcceptanceCriterionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAcceptanceCriterionSample1();
        var actual = acceptanceCriterionMapper.toEntity(acceptanceCriterionMapper.toDto(expected));
        assertAcceptanceCriterionAllPropertiesEquals(expected, actual);
    }
}
