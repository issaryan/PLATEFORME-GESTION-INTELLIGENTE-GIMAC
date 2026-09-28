package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.UserSessionAsserts.*;
import static cm.gimac.des.iam.domain.UserSessionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSessionMapperTest {

    private UserSessionMapper userSessionMapper;

    @BeforeEach
    void setUp() {
        userSessionMapper = new UserSessionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getUserSessionSample1();
        var actual = userSessionMapper.toEntity(userSessionMapper.toDto(expected));
        assertUserSessionAllPropertiesEquals(expected, actual);
    }
}
