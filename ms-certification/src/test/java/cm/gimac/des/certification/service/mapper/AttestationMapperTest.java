package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.AttestationAsserts.*;
import static cm.gimac.des.certification.domain.AttestationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttestationMapperTest {

    private AttestationMapper attestationMapper;

    @BeforeEach
    void setUp() {
        attestationMapper = new AttestationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAttestationSample1();
        var actual = attestationMapper.toEntity(attestationMapper.toDto(expected));
        assertAttestationAllPropertiesEquals(expected, actual);
    }
}
