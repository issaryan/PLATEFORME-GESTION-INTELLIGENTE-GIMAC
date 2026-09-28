package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PermissionDTO.class);
        PermissionDTO permissionDTO1 = new PermissionDTO();
        permissionDTO1.setId(UUID.randomUUID());
        PermissionDTO permissionDTO2 = new PermissionDTO();
        assertThat(permissionDTO1).isNotEqualTo(permissionDTO2);
        permissionDTO2.setId(permissionDTO1.getId());
        assertThat(permissionDTO1).isEqualTo(permissionDTO2);
        permissionDTO2.setId(UUID.randomUUID());
        assertThat(permissionDTO1).isNotEqualTo(permissionDTO2);
        permissionDTO1.setId(null);
        assertThat(permissionDTO1).isNotEqualTo(permissionDTO2);
    }
}
