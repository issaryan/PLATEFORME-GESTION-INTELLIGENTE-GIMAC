package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleGrantDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoleGrantDTO.class);
        RoleGrantDTO roleGrantDTO1 = new RoleGrantDTO();
        roleGrantDTO1.setId(UUID.randomUUID());
        RoleGrantDTO roleGrantDTO2 = new RoleGrantDTO();
        assertThat(roleGrantDTO1).isNotEqualTo(roleGrantDTO2);
        roleGrantDTO2.setId(roleGrantDTO1.getId());
        assertThat(roleGrantDTO1).isEqualTo(roleGrantDTO2);
        roleGrantDTO2.setId(UUID.randomUUID());
        assertThat(roleGrantDTO1).isNotEqualTo(roleGrantDTO2);
        roleGrantDTO1.setId(null);
        assertThat(roleGrantDTO1).isNotEqualTo(roleGrantDTO2);
    }
}
