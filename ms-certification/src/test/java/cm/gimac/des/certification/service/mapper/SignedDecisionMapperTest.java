package cm.gimac.des.certification.service.mapper;

import static cm.gimac.des.certification.domain.SignedDecisionAsserts.*;
import static cm.gimac.des.certification.domain.SignedDecisionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignedDecisionMapperTest {

    private SignedDecisionMapper signedDecisionMapper;

    @BeforeEach
    void setUp() {
        signedDecisionMapper = new SignedDecisionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSignedDecisionSample1();
        var actual = signedDecisionMapper.toEntity(signedDecisionMapper.toDto(expected));
        assertSignedDecisionAllPropertiesEquals(expected, actual);
    }
}
