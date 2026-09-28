package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.AssignmentTestSamples.*;
import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AssignmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Assignment.class);
        Assignment assignment1 = getAssignmentSample1();
        Assignment assignment2 = new Assignment();
        assertThat(assignment1).isNotEqualTo(assignment2);

        assignment2.setId(assignment1.getId());
        assertThat(assignment1).isEqualTo(assignment2);

        assignment2 = getAssignmentSample2();
        assertThat(assignment1).isNotEqualTo(assignment2);
    }

    @Test
    void collaboratorTest() {
        Assignment assignment = getAssignmentRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        assignment.setCollaborator(collaboratorBack);
        assertThat(assignment.getCollaborator()).isEqualTo(collaboratorBack);

        assignment.collaborator(null);
        assertThat(assignment.getCollaborator()).isNull();
    }
}
