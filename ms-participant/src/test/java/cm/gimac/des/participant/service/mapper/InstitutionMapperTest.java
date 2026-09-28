package cm.gimac.des.participant.service.mapper;

import static cm.gimac.des.participant.domain.InstitutionAsserts.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstitutionMapperTest {

    private InstitutionMapper institutionMapper;

    @BeforeEach
    void setUp() {
        institutionMapper = new InstitutionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getInstitutionSample1();
        var actual = institutionMapper.toEntity(institutionMapper.toDto(expected));
        assertInstitutionAllPropertiesEquals(expected, actual);
    }
}
