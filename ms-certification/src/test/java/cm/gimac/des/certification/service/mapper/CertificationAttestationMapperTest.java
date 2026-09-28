package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CertificationAttestationAsserts.*;
import static cm.gimac.des.certification.domain.CertificationAttestationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificationAttestationMapperTest {

    private CertificationAttestationMapper certificationAttestationMapper;

    @BeforeEach
    void setUp() {
        certificationAttestationMapper = new CertificationAttestationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCertificationAttestationSample1();
        var actual = certificationAttestationMapper.toEntity(certificationAttestationMapper.toDto(expected));
        assertCertificationAttestationAllPropertiesEquals(expected, actual);
    }
}
