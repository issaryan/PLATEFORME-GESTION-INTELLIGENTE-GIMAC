package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.QualificationMatrixAsserts.*;
import static cm.gimac.des.incident.domain.QualificationMatrixTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QualificationMatrixMapperTest {

    private QualificationMatrixMapper qualificationMatrixMapper;

    @BeforeEach
    void setUp() {
        qualificationMatrixMapper = new QualificationMatrixMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getQualificationMatrixSample1();
        var actual = qualificationMatrixMapper.toEntity(qualificationMatrixMapper.toDto(expected));
        assertQualificationMatrixAllPropertiesEquals(expected, actual);
    }
}
