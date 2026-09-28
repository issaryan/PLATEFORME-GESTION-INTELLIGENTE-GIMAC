package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.IntegrityCheckAsserts.*;
import static cm.gimac.des.document.domain.IntegrityCheckTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrityCheckMapperTest {

    private IntegrityCheckMapper integrityCheckMapper;

    @BeforeEach
    void setUp() {
        integrityCheckMapper = new IntegrityCheckMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIntegrityCheckSample1();
        var actual = integrityCheckMapper.toEntity(integrityCheckMapper.toDto(expected));
        assertIntegrityCheckAllPropertiesEquals(expected, actual);
    }
}
