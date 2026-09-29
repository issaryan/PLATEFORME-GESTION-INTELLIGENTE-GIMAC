package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ReferentialMigrationAsserts.*;
import static cm.gimac.des.homologation.domain.ReferentialMigrationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferentialMigrationMapperTest {

    private ReferentialMigrationMapper referentialMigrationMapper;

    @BeforeEach
    void setUp() {
        referentialMigrationMapper = new ReferentialMigrationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReferentialMigrationSample1();
        var actual = referentialMigrationMapper.toEntity(referentialMigrationMapper.toDto(expected));
        assertReferentialMigrationAllPropertiesEquals(expected, actual);
    }
}
