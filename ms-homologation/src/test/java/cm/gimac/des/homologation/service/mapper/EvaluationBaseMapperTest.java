package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.EvaluationBaseAsserts.*;
import static cm.gimac.des.homologation.domain.EvaluationBaseTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvaluationBaseMapperTest {

    private EvaluationBaseMapper evaluationBaseMapper;

    @BeforeEach
    void setUp() {
        evaluationBaseMapper = new EvaluationBaseMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvaluationBaseSample1();
        var actual = evaluationBaseMapper.toEntity(evaluationBaseMapper.toDto(expected));
        assertEvaluationBaseAllPropertiesEquals(expected, actual);
    }
}
