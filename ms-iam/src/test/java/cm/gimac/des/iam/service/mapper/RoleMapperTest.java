package cm.gimac.des.iam.service.mapper;

import static cm.gimac.des.iam.domain.RoleAsserts.*;
import static cm.gimac.des.iam.domain.RoleTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleMapperTest {

    private RoleMapper roleMapper;

    @BeforeEach
    void setUp() {
        roleMapper = new RoleMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getRoleSample1();
        var actual = roleMapper.toEntity(roleMapper.toDto(expected));
        assertRoleAllPropertiesEquals(expected, actual);
    }
}
