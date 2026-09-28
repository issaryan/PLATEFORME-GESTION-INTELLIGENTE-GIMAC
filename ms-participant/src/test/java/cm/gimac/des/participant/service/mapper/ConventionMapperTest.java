package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.ConventionAsserts.*;
import static cm.gimac.des.participant.domain.ConventionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConventionMapperTest {

    private ConventionMapper conventionMapper;

    @BeforeEach
    void setUp() {
        conventionMapper = new ConventionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getConventionSample1();
        var actual = conventionMapper.toEntity(conventionMapper.toDto(expected));
        assertConventionAllPropertiesEquals(expected, actual);
    }
}
