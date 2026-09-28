package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.LiaisonAsserts.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiaisonMapperTest {

    private LiaisonMapper liaisonMapper;

    @BeforeEach
    void setUp() {
        liaisonMapper = new LiaisonMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getLiaisonSample1();
        var actual = liaisonMapper.toEntity(liaisonMapper.toDto(expected));
        assertLiaisonAllPropertiesEquals(expected, actual);
    }
}
