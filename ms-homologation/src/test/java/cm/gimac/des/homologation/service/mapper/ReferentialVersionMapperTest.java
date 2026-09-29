package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ReferentialVersionAsserts.*;
import static cm.gimac.des.homologation.domain.ReferentialVersionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferentialVersionMapperTest {

    private ReferentialVersionMapper referentialVersionMapper;

    @BeforeEach
    void setUp() {
        referentialVersionMapper = new ReferentialVersionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReferentialVersionSample1();
        var actual = referentialVersionMapper.toEntity(referentialVersionMapper.toDto(expected));
        assertReferentialVersionAllPropertiesEquals(expected, actual);
    }
}
