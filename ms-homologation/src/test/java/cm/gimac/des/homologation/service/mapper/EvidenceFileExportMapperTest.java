package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.EvidenceFileExportAsserts.*;
import static cm.gimac.des.homologation.domain.EvidenceFileExportTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceFileExportMapperTest {

    private EvidenceFileExportMapper evidenceFileExportMapper;

    @BeforeEach
    void setUp() {
        evidenceFileExportMapper = new EvidenceFileExportMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEvidenceFileExportSample1();
        var actual = evidenceFileExportMapper.toEntity(evidenceFileExportMapper.toDto(expected));
        assertEvidenceFileExportAllPropertiesEquals(expected, actual);
    }
}
