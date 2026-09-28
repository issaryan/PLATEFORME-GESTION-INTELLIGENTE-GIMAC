package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.EvidenceItemAsserts.*;
import static cm.gimac.des.certification.domain.EvidenceItemTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceItemMapperTest {

    private EvidenceItemMapper evidenceItemMapper;

    @BeforeEach
    void setUp() {
        evidenceItemMapper = new EvidenceItemMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvidenceItemSample1();
        var actual = evidenceItemMapper.toEntity(evidenceItemMapper.toDto(expected));
        assertEvidenceItemAllPropertiesEquals(expected, actual);
    }
}
