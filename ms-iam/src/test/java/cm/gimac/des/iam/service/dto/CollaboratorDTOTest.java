package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollaboratorDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CollaboratorDTO.class);
        CollaboratorDTO collaboratorDTO1 = new CollaboratorDTO();
        collaboratorDTO1.setId(UUID.randomUUID());
        CollaboratorDTO collaboratorDTO2 = new CollaboratorDTO();
        assertThat(collaboratorDTO1).isNotEqualTo(collaboratorDTO2);
        collaboratorDTO2.setId(collaboratorDTO1.getId());
        assertThat(collaboratorDTO1).isEqualTo(collaboratorDTO2);
        collaboratorDTO2.setId(UUID.randomUUID());
        assertThat(collaboratorDTO1).isNotEqualTo(collaboratorDTO2);
        collaboratorDTO1.setId(null);
        assertThat(collaboratorDTO1).isNotEqualTo(collaboratorDTO2);
    }
}
