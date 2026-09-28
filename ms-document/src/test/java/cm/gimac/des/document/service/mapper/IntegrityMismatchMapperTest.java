package cm.gimac.des.document.service.mapper;

import static cm.gimac.des.document.domain.IntegrityMismatchAsserts.*;
import static cm.gimac.des.document.domain.IntegrityMismatchTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrityMismatchMapperTest {

    private IntegrityMismatchMapper integrityMismatchMapper;

    @BeforeEach
    void setUp() {
        integrityMismatchMapper = new IntegrityMismatchMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getIntegrityMismatchSample1();
        var actual = integrityMismatchMapper.toEntity(integrityMismatchMapper.toDto(expected));
        assertIntegrityMismatchAllPropertiesEquals(expected, actual);
    }
}
