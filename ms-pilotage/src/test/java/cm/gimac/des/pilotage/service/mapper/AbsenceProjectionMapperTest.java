package cm.gimac.des.pilotage.service.mapper;

import static cm.gimac.des.pilotage.domain.AbsenceProjectionAsserts.*;
import static cm.gimac.des.pilotage.domain.AbsenceProjectionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbsenceProjectionMapperTest {

    private AbsenceProjectionMapper absenceProjectionMapper;

    @BeforeEach
    void setUp() {
        absenceProjectionMapper = new AbsenceProjectionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAbsenceProjectionSample1();
        var actual = absenceProjectionMapper.toEntity(absenceProjectionMapper.toDto(expected));
        assertAbsenceProjectionAllPropertiesEquals(expected, actual);
    }
}
