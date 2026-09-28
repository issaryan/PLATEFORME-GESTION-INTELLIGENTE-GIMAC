package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollaboratorProjectionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CollaboratorProjectionDTO.class);
        CollaboratorProjectionDTO collaboratorProjectionDTO1 = new CollaboratorProjectionDTO();
        collaboratorProjectionDTO1.setId(UUID.randomUUID());
        CollaboratorProjectionDTO collaboratorProjectionDTO2 = new CollaboratorProjectionDTO();
        assertThat(collaboratorProjectionDTO1).isNotEqualTo(collaboratorProjectionDTO2);
        collaboratorProjectionDTO2.setId(collaboratorProjectionDTO1.getId());
        assertThat(collaboratorProjectionDTO1).isEqualTo(collaboratorProjectionDTO2);
        collaboratorProjectionDTO2.setId(UUID.randomUUID());
        assertThat(collaboratorProjectionDTO1).isNotEqualTo(collaboratorProjectionDTO2);
        collaboratorProjectionDTO1.setId(null);
        assertThat(collaboratorProjectionDTO1).isNotEqualTo(collaboratorProjectionDTO2);
    }
}
