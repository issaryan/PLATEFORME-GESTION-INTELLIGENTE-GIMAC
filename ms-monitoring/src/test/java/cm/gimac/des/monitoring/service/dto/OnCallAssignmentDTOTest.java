package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnCallAssignmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OnCallAssignmentDTO.class);
        OnCallAssignmentDTO onCallAssignmentDTO1 = new OnCallAssignmentDTO();
        onCallAssignmentDTO1.setId(UUID.randomUUID());
        OnCallAssignmentDTO onCallAssignmentDTO2 = new OnCallAssignmentDTO();
        assertThat(onCallAssignmentDTO1).isNotEqualTo(onCallAssignmentDTO2);
        onCallAssignmentDTO2.setId(onCallAssignmentDTO1.getId());
        assertThat(onCallAssignmentDTO1).isEqualTo(onCallAssignmentDTO2);
        onCallAssignmentDTO2.setId(UUID.randomUUID());
        assertThat(onCallAssignmentDTO1).isNotEqualTo(onCallAssignmentDTO2);
        onCallAssignmentDTO1.setId(null);
        assertThat(onCallAssignmentDTO1).isNotEqualTo(onCallAssignmentDTO2);
    }
}
