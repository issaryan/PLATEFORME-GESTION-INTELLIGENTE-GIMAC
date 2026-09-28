package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static cm.gimac.des.iam.domain.DelegationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DelegationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Delegation.class);
        Delegation delegation1 = getDelegationSample1();
        Delegation delegation2 = new Delegation();
        assertThat(delegation1).isNotEqualTo(delegation2);

        delegation2.setId(delegation1.getId());
        assertThat(delegation1).isEqualTo(delegation2);

        delegation2 = getDelegationSample2();
        assertThat(delegation1).isNotEqualTo(delegation2);
    }

    @Test
    void collaboratorTest() {
        Delegation delegation = getDelegationRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        delegation.setCollaborator(collaboratorBack);
        assertThat(delegation.getCollaborator()).isEqualTo(collaboratorBack);

        delegation.collaborator(null);
        assertThat(delegation.getCollaborator()).isNull();
    }
}
