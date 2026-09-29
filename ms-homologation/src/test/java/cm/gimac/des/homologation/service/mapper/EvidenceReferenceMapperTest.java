package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.EvidenceReferenceAsserts.*;
import static cm.gimac.des.homologation.domain.EvidenceReferenceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceReferenceMapperTest {

    private EvidenceReferenceMapper evidenceReferenceMapper;

    @BeforeEach
    void setUp() {
        evidenceReferenceMapper = new EvidenceReferenceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvidenceReferenceSample1();
        var actual = evidenceReferenceMapper.toEntity(evidenceReferenceMapper.toDto(expected));
        assertEvidenceReferenceAllPropertiesEquals(expected, actual);
    }
}
