package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserSessionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(UserSessionDTO.class);
        UserSessionDTO userSessionDTO1 = new UserSessionDTO();
        userSessionDTO1.setId(UUID.randomUUID());
        UserSessionDTO userSessionDTO2 = new UserSessionDTO();
        assertThat(userSessionDTO1).isNotEqualTo(userSessionDTO2);
        userSessionDTO2.setId(userSessionDTO1.getId());
        assertThat(userSessionDTO1).isEqualTo(userSessionDTO2);
        userSessionDTO2.setId(UUID.randomUUID());
        assertThat(userSessionDTO1).isNotEqualTo(userSessionDTO2);
        userSessionDTO1.setId(null);
        assertThat(userSessionDTO1).isNotEqualTo(userSessionDTO2);
    }
}
