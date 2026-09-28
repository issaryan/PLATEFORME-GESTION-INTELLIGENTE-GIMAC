package cm.gimac.des.incident.service.mapper;

import static cm.gimac.des.incident.domain.CrisisCellAsserts.*;
import static cm.gimac.des.incident.domain.CrisisCellTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrisisCellMapperTest {

    private CrisisCellMapper crisisCellMapper;

    @BeforeEach
    void setUp() {
        crisisCellMapper = new CrisisCellMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCrisisCellSample1();
        var actual = crisisCellMapper.toEntity(crisisCellMapper.toDto(expected));
        assertCrisisCellAllPropertiesEquals(expected, actual);
    }
}
