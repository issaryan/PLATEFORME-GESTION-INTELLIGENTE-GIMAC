package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.TestScenarioAsserts.*;
import static cm.gimac.des.homologation.domain.TestScenarioTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestScenarioMapperTest {

    private TestScenarioMapper testScenarioMapper;

    @BeforeEach
    void setUp() {
        testScenarioMapper = new TestScenarioMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getTestScenarioSample1();
        var actual = testScenarioMapper.toEntity(testScenarioMapper.toDto(expected));
        assertTestScenarioAllPropertiesEquals(expected, actual);
    }
}
