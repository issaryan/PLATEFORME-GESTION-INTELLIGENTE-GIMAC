package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.PermissionTestSamples.*;
import static cm.gimac.des.iam.domain.RoleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Role.class);
        Role role1 = getRoleSample1();
        Role role2 = new Role();
        assertThat(role1).isNotEqualTo(role2);

        role2.setId(role1.getId());
        assertThat(role1).isEqualTo(role2);

        role2 = getRoleSample2();
        assertThat(role1).isNotEqualTo(role2);
    }

    @Test
    void permissionsTest() {
        Role role = getRoleRandomSampleGenerator();
        Permission permissionBack = getPermissionRandomSampleGenerator();

        role.addPermissions(permissionBack);
        assertThat(role.getPermissionses()).containsOnly(permissionBack);

        role.removePermissions(permissionBack);
        assertThat(role.getPermissionses()).doesNotContain(permissionBack);

        role.permissionses(new HashSet<>(Set.of(permissionBack)));
        assertThat(role.getPermissionses()).containsOnly(permissionBack);

        role.setPermissionses(new HashSet<>());
        assertThat(role.getPermissionses()).doesNotContain(permissionBack);
    }
}
