package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CompletenessConditionAsserts.*;
import static cm.gimac.des.certification.domain.CompletenessConditionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompletenessConditionMapperTest {

    private CompletenessConditionMapper completenessConditionMapper;

    @BeforeEach
    void setUp() {
        completenessConditionMapper = new CompletenessConditionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCompletenessConditionSample1();
        var actual = completenessConditionMapper.toEntity(completenessConditionMapper.toDto(expected));
        assertCompletenessConditionAllPropertiesEquals(expected, actual);
    }
}
