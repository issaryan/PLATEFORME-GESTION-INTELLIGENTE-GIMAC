package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ReferenceOperationAsserts.*;
import static cm.gimac.des.homologation.domain.ReferenceOperationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferenceOperationMapperTest {

    private ReferenceOperationMapper referenceOperationMapper;

    @BeforeEach
    void setUp() {
        referenceOperationMapper = new ReferenceOperationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReferenceOperationSample1();
        var actual = referenceOperationMapper.toEntity(referenceOperationMapper.toDto(expected));
        assertReferenceOperationAllPropertiesEquals(expected, actual);
    }
}
