package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.CrisisMemberAsserts.*;
import static cm.gimac.des.incident.domain.CrisisMemberTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrisisMemberMapperTest {

    private CrisisMemberMapper crisisMemberMapper;

    @BeforeEach
    void setUp() {
        crisisMemberMapper = new CrisisMemberMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCrisisMemberSample1();
        var actual = crisisMemberMapper.toEntity(crisisMemberMapper.toDto(expected));
        assertCrisisMemberAllPropertiesEquals(expected, actual);
    }
}
