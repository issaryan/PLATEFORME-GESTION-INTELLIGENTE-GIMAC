package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.UnmatchedScenarioAsserts.*;
import static cm.gimac.des.homologation.domain.UnmatchedScenarioTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnmatchedScenarioMapperTest {

    private UnmatchedScenarioMapper unmatchedScenarioMapper;

    @BeforeEach
    void setUp() {
        unmatchedScenarioMapper = new UnmatchedScenarioMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getUnmatchedScenarioSample1();
        var actual = unmatchedScenarioMapper.toEntity(unmatchedScenarioMapper.toDto(expected));
        assertUnmatchedScenarioAllPropertiesEquals(expected, actual);
    }
}
