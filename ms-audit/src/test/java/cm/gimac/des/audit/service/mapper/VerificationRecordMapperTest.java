package cm.gimac.des.audit.service.mapper;

import static cm.gimac.des.audit.domain.VerificationRecordAsserts.*;
import static cm.gimac.des.audit.domain.VerificationRecordTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerificationRecordMapperTest {

    private VerificationRecordMapper verificationRecordMapper;

    @BeforeEach
    void setUp() {
        verificationRecordMapper = new VerificationRecordMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getVerificationRecordSample1();
        var actual = verificationRecordMapper.toEntity(verificationRecordMapper.toDto(expected));
        assertVerificationRecordAllPropertiesEquals(expected, actual);
    }
}
