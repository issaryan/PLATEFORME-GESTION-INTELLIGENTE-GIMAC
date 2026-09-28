package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.AccessReviewAsserts.*;
import static cm.gimac.des.iam.domain.AccessReviewTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessReviewMapperTest {

    private AccessReviewMapper accessReviewMapper;

    @BeforeEach
    void setUp() {
        accessReviewMapper = new AccessReviewMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAccessReviewSample1();
        var actual = accessReviewMapper.toEntity(accessReviewMapper.toDto(expected));
        assertAccessReviewAllPropertiesEquals(expected, actual);
    }
}
