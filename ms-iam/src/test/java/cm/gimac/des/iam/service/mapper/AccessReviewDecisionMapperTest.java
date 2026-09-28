package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.AccessReviewDecisionAsserts.*;
import static cm.gimac.des.iam.domain.AccessReviewDecisionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessReviewDecisionMapperTest {

    private AccessReviewDecisionMapper accessReviewDecisionMapper;

    @BeforeEach
    void setUp() {
        accessReviewDecisionMapper = new AccessReviewDecisionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAccessReviewDecisionSample1();
        var actual = accessReviewDecisionMapper.toEntity(accessReviewDecisionMapper.toDto(expected));
        assertAccessReviewDecisionAllPropertiesEquals(expected, actual);
    }
}
