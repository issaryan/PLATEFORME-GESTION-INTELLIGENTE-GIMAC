package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.ReferenceCatalogAsserts.*;
import static cm.gimac.des.homologation.domain.ReferenceCatalogTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferenceCatalogMapperTest {

    private ReferenceCatalogMapper referenceCatalogMapper;

    @BeforeEach
    void setUp() {
        referenceCatalogMapper = new ReferenceCatalogMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getReferenceCatalogSample1();
        var actual = referenceCatalogMapper.toEntity(referenceCatalogMapper.toDto(expected));
        assertReferenceCatalogAllPropertiesEquals(expected, actual);
    }
}
