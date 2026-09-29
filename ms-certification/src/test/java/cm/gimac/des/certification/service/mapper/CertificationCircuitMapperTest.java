package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CertificationCircuitAsserts.*;
import static cm.gimac.des.certification.domain.CertificationCircuitTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificationCircuitMapperTest {

    private CertificationCircuitMapper certificationCircuitMapper;

    @BeforeEach
    void setUp() {
        certificationCircuitMapper = new CertificationCircuitMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCertificationCircuitSample1();
        var actual = certificationCircuitMapper.toEntity(certificationCircuitMapper.toDto(expected));
        assertCertificationCircuitAllPropertiesEquals(expected, actual);
    }
}
