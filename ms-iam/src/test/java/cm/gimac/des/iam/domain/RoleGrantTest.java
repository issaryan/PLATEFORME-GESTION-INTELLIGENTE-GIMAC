package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static cm.gimac.des.iam.domain.RoleGrantTestSamples.*;
import static cm.gimac.des.iam.domain.RoleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoleGrantTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoleGrant.class);
        RoleGrant roleGrant1 = getRoleGrantSample1();
        RoleGrant roleGrant2 = new RoleGrant();
        assertThat(roleGrant1).isNotEqualTo(roleGrant2);

        roleGrant2.setId(roleGrant1.getId());
        assertThat(roleGrant1).isEqualTo(roleGrant2);

        roleGrant2 = getRoleGrantSample2();
        assertThat(roleGrant1).isNotEqualTo(roleGrant2);
    }

    @Test
    void collaboratorTest() {
        RoleGrant roleGrant = getRoleGrantRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        roleGrant.setCollaborator(collaboratorBack);
        assertThat(roleGrant.getCollaborator()).isEqualTo(collaboratorBack);

        roleGrant.collaborator(null);
        assertThat(roleGrant.getCollaborator()).isNull();
    }

    @Test
    void roleTest() {
        RoleGrant roleGrant = getRoleGrantRandomSampleGenerator();
        Role roleBack = getRoleRandomSampleGenerator();

        roleGrant.setRole(roleBack);
        assertThat(roleGrant.getRole()).isEqualTo(roleBack);

        roleGrant.role(null);
        assertThat(roleGrant.getRole()).isNull();
    }
}
