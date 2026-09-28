package cm.gimac.des.homologation.service.mapper;

import static cm.gimac.des.homologation.domain.QrVerificationAsserts.*;
import static cm.gimac.des.homologation.domain.QrVerificationTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QrVerificationMapperTest {

    private QrVerificationMapper qrVerificationMapper;

    @BeforeEach
    void setUp() {
        qrVerificationMapper = new QrVerificationMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getQrVerificationSample1();
        var actual = qrVerificationMapper.toEntity(qrVerificationMapper.toDto(expected));
        assertQrVerificationAllPropertiesEquals(expected, actual);
    }
}
