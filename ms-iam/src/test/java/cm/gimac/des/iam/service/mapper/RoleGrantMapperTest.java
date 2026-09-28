package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.RoleGrantAsserts.*;
import static cm.gimac.des.iam.domain.RoleGrantTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleGrantMapperTest {

    private RoleGrantMapper roleGrantMapper;

    @BeforeEach
    void setUp() {
        roleGrantMapper = new RoleGrantMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoleGrantSample1();
        var actual = roleGrantMapper.toEntity(roleGrantMapper.toDto(expected));
        assertRoleGrantAllPropertiesEquals(expected, actual);
    }
}
