package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.InclusionProofAsserts.*;
import static cm.gimac.des.audit.domain.InclusionProofTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InclusionProofMapperTest {

    private InclusionProofMapper inclusionProofMapper;

    @BeforeEach
    void setUp() {
        inclusionProofMapper = new InclusionProofMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getInclusionProofSample1();
        var actual = inclusionProofMapper.toEntity(inclusionProofMapper.toDto(expected));
        assertInclusionProofAllPropertiesEquals(expected, actual);
    }
}
