package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.PermissionTestSamples.*;
import static cm.gimac.des.iam.domain.RoleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Permission.class);
        Permission permission1 = getPermissionSample1();
        Permission permission2 = new Permission();
        assertThat(permission1).isNotEqualTo(permission2);

        permission2.setId(permission1.getId());
        assertThat(permission1).isEqualTo(permission2);

        permission2 = getPermissionSample2();
        assertThat(permission1).isNotEqualTo(permission2);
    }

    @Test
    void rolesTest() {
        Permission permission = getPermissionRandomSampleGenerator();
        Role roleBack = getRoleRandomSampleGenerator();

        permission.addRoles(roleBack);
        assertThat(permission.getRoleses()).containsOnly(roleBack);
        assertThat(roleBack.getPermissionses()).containsOnly(permission);

        permission.removeRoles(roleBack);
        assertThat(permission.getRoleses()).doesNotContain(roleBack);
        assertThat(roleBack.getPermissionses()).doesNotContain(permission);

        permission.roleses(new HashSet<>(Set.of(roleBack)));
        assertThat(permission.getRoleses()).containsOnly(roleBack);
        assertThat(roleBack.getPermissionses()).containsOnly(permission);

        permission.setRoleses(new HashSet<>());
        assertThat(permission.getRoleses()).doesNotContain(roleBack);
        assertThat(roleBack.getPermissionses()).doesNotContain(permission);
    }
}
