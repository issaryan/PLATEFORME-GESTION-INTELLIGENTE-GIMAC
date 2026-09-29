package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.AttestationStatusDecisionAsserts.*;
import static cm.gimac.des.certification.domain.AttestationStatusDecisionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttestationStatusDecisionMapperTest {

    private AttestationStatusDecisionMapper attestationStatusDecisionMapper;

    @BeforeEach
    void setUp() {
        attestationStatusDecisionMapper = new AttestationStatusDecisionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAttestationStatusDecisionSample1();
        var actual = attestationStatusDecisionMapper.toEntity(attestationStatusDecisionMapper.toDto(expected));
        assertAttestationStatusDecisionAllPropertiesEquals(expected, actual);
    }
}
