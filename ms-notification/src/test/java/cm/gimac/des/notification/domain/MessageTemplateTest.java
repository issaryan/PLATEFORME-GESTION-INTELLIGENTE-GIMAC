package cm.gimac.des.notification.domain;

import static cm.gimac.des.notification.domain.MessageTemplateTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MessageTemplateTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageTemplate.class);
        MessageTemplate messageTemplate1 = getMessageTemplateSample1();
        MessageTemplate messageTemplate2 = new MessageTemplate();
        assertThat(messageTemplate1).isNotEqualTo(messageTemplate2);

        messageTemplate2.setId(messageTemplate1.getId());
        assertThat(messageTemplate1).isEqualTo(messageTemplate2);

        messageTemplate2 = getMessageTemplateSample2();
        assertThat(messageTemplate1).isNotEqualTo(messageTemplate2);
    }
}
