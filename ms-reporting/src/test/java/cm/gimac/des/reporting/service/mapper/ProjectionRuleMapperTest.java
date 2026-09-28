package cm.gimac.des.reporting.service.mapper;

import static cm.gimac.des.reporting.domain.ProjectionRuleAsserts.*;
import static cm.gimac.des.reporting.domain.ProjectionRuleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectionRuleMapperTest {

    private ProjectionRuleMapper projectionRuleMapper;

    @BeforeEach
    void setUp() {
        projectionRuleMapper = new ProjectionRuleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getProjectionRuleSample1();
        var actual = projectionRuleMapper.toEntity(projectionRuleMapper.toDto(expected));
        assertProjectionRuleAllPropertiesEquals(expected, actual);
    }
}
