package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.CoverageAsserts.*;
import static cm.gimac.des.homologation.domain.CoverageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoverageMapperTest {

    private CoverageMapper coverageMapper;

    @BeforeEach
    void setUp() {
        coverageMapper = new CoverageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCoverageSample1();
        var actual = coverageMapper.toEntity(coverageMapper.toDto(expected));
        assertCoverageAllPropertiesEquals(expected, actual);
    }
}
