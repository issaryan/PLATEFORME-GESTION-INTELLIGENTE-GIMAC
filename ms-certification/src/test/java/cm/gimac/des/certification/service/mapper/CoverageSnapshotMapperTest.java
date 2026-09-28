package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CoverageSnapshotAsserts.*;
import static cm.gimac.des.certification.domain.CoverageSnapshotTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoverageSnapshotMapperTest {

    private CoverageSnapshotMapper coverageSnapshotMapper;

    @BeforeEach
    void setUp() {
        coverageSnapshotMapper = new CoverageSnapshotMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCoverageSnapshotSample1();
        var actual = coverageSnapshotMapper.toEntity(coverageSnapshotMapper.toDto(expected));
        assertCoverageSnapshotAllPropertiesEquals(expected, actual);
    }
}
