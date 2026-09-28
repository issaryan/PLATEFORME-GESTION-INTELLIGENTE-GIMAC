package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AssignmentDTO.class);
        AssignmentDTO assignmentDTO1 = new AssignmentDTO();
        assignmentDTO1.setId(UUID.randomUUID());
        AssignmentDTO assignmentDTO2 = new AssignmentDTO();
        assertThat(assignmentDTO1).isNotEqualTo(assignmentDTO2);
        assignmentDTO2.setId(assignmentDTO1.getId());
        assertThat(assignmentDTO1).isEqualTo(assignmentDTO2);
        assignmentDTO2.setId(UUID.randomUUID());
        assertThat(assignmentDTO1).isNotEqualTo(assignmentDTO2);
        assignmentDTO1.setId(null);
        assertThat(assignmentDTO1).isNotEqualTo(assignmentDTO2);
    }
}
