package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.DeadlineNotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DeadlineNotificationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeadlineNotification.class);
        DeadlineNotification deadlineNotification1 = getDeadlineNotificationSample1();
        DeadlineNotification deadlineNotification2 = new DeadlineNotification();
        assertThat(deadlineNotification1).isNotEqualTo(deadlineNotification2);

        deadlineNotification2.setId(deadlineNotification1.getId());
        assertThat(deadlineNotification1).isEqualTo(deadlineNotification2);

        deadlineNotification2 = getDeadlineNotificationSample2();
        assertThat(deadlineNotification1).isNotEqualTo(deadlineNotification2);
    }
}
