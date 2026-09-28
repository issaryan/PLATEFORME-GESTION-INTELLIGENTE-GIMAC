package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomRetentionPolicyDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomRetentionPolicyDTO.class);
        RoomRetentionPolicyDTO roomRetentionPolicyDTO1 = new RoomRetentionPolicyDTO();
        roomRetentionPolicyDTO1.setId(UUID.randomUUID());
        RoomRetentionPolicyDTO roomRetentionPolicyDTO2 = new RoomRetentionPolicyDTO();
        assertThat(roomRetentionPolicyDTO1).isNotEqualTo(roomRetentionPolicyDTO2);
        roomRetentionPolicyDTO2.setId(roomRetentionPolicyDTO1.getId());
        assertThat(roomRetentionPolicyDTO1).isEqualTo(roomRetentionPolicyDTO2);
        roomRetentionPolicyDTO2.setId(UUID.randomUUID());
        assertThat(roomRetentionPolicyDTO1).isNotEqualTo(roomRetentionPolicyDTO2);
        roomRetentionPolicyDTO1.setId(null);
        assertThat(roomRetentionPolicyDTO1).isNotEqualTo(roomRetentionPolicyDTO2);
    }
}
