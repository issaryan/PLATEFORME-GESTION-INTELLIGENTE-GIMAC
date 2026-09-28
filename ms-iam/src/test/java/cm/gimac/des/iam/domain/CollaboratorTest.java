package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CollaboratorTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Collaborator.class);
        Collaborator collaborator1 = getCollaboratorSample1();
        Collaborator collaborator2 = new Collaborator();
        assertThat(collaborator1).isNotEqualTo(collaborator2);

        collaborator2.setId(collaborator1.getId());
        assertThat(collaborator1).isEqualTo(collaborator2);

        collaborator2 = getCollaboratorSample2();
        assertThat(collaborator1).isNotEqualTo(collaborator2);
    }
}
