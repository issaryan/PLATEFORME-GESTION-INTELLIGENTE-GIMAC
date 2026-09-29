package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.CertificationRegisterEntryAsserts.*;
import static cm.gimac.des.certification.domain.CertificationRegisterEntryTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificationRegisterEntryMapperTest {

    private CertificationRegisterEntryMapper certificationRegisterEntryMapper;

    @BeforeEach
    void setUp() {
        certificationRegisterEntryMapper = new CertificationRegisterEntryMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCertificationRegisterEntrySample1();
        var actual = certificationRegisterEntryMapper.toEntity(certificationRegisterEntryMapper.toDto(expected));
        assertCertificationRegisterEntryAllPropertiesEquals(expected, actual);
    }
}
