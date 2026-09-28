package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.ConsolidationWindowTestSamples.*;
import static cm.gimac.des.notification.domain.NotificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConsolidationWindowTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConsolidationWindow.class);
        ConsolidationWindow consolidationWindow1 = getConsolidationWindowSample1();
        ConsolidationWindow consolidationWindow2 = new ConsolidationWindow();
        assertThat(consolidationWindow1).isNotEqualTo(consolidationWindow2);

        consolidationWindow2.setId(consolidationWindow1.getId());
        assertThat(consolidationWindow1).isEqualTo(consolidationWindow2);

        consolidationWindow2 = getConsolidationWindowSample2();
        assertThat(consolidationWindow1).isNotEqualTo(consolidationWindow2);
    }

    @Test
    void summaryNotificationTest() {
        ConsolidationWindow consolidationWindow = getConsolidationWindowRandomSampleGenerator();
        Notification notificationBack = getNotificationRandomSampleGenerator();

        consolidationWindow.setSummaryNotification(notificationBack);
        assertThat(consolidationWindow.getSummaryNotification()).isEqualTo(notificationBack);

        consolidationWindow.summaryNotification(null);
        assertThat(consolidationWindow.getSummaryNotification()).isNull();
    }
}
