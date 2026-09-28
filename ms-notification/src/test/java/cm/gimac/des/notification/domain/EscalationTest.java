package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.EscalationTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EscalationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Escalation.class);
        Escalation escalation1 = getEscalationSample1();
        Escalation escalation2 = new Escalation();
        assertThat(escalation1).isNotEqualTo(escalation2);

        escalation2.setId(escalation1.getId());
        assertThat(escalation1).isEqualTo(escalation2);

        escalation2 = getEscalationSample2();
        assertThat(escalation1).isNotEqualTo(escalation2);
    }

    @Test
    void notificationTest() {
        Escalation escalation = getEscalationRandomSampleGenerator();
        Notification notificationBack = getNotificationRandomSampleGenerator();

        escalation.setNotification(notificationBack);
        assertThat(escalation.getNotification()).isEqualTo(notificationBack);

        escalation.notification(null);
        assertThat(escalation.getNotification()).isNull();
    }
}
