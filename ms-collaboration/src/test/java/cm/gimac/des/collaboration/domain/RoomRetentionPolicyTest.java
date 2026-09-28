package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.RoomRetentionPolicyTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RoomRetentionPolicyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomRetentionPolicy.class);
        RoomRetentionPolicy roomRetentionPolicy1 = getRoomRetentionPolicySample1();
        RoomRetentionPolicy roomRetentionPolicy2 = new RoomRetentionPolicy();
        assertThat(roomRetentionPolicy1).isNotEqualTo(roomRetentionPolicy2);

        roomRetentionPolicy2.setId(roomRetentionPolicy1.getId());
        assertThat(roomRetentionPolicy1).isEqualTo(roomRetentionPolicy2);

        roomRetentionPolicy2 = getRoomRetentionPolicySample2();
        assertThat(roomRetentionPolicy1).isNotEqualTo(roomRetentionPolicy2);
    }
}
