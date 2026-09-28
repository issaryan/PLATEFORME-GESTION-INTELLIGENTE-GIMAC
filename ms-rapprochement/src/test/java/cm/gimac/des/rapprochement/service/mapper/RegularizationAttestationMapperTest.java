package cm.gimac.des.rapprochement.service.mapper;

import static cm.gimac.des.rapprochement.domain.RegularizationAttestationAsserts.*;
import static cm.gimac.des.rapprochement.domain.RegularizationAttestationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegularizationAttestationMapperTest {

    private RegularizationAttestationMapper regularizationAttestationMapper;

    @BeforeEach
    void setUp() {
        regularizationAttestationMapper = new RegularizationAttestationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRegularizationAttestationSample1();
        var actual = regularizationAttestationMapper.toEntity(regularizationAttestationMapper.toDto(expected));
        assertRegularizationAttestationAllPropertiesEquals(expected, actual);
    }
}
