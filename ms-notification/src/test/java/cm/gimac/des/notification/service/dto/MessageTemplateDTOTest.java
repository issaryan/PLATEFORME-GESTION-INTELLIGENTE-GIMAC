package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageTemplateDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageTemplateDTO.class);
        MessageTemplateDTO messageTemplateDTO1 = new MessageTemplateDTO();
        messageTemplateDTO1.setId(UUID.randomUUID());
        MessageTemplateDTO messageTemplateDTO2 = new MessageTemplateDTO();
        assertThat(messageTemplateDTO1).isNotEqualTo(messageTemplateDTO2);
        messageTemplateDTO2.setId(messageTemplateDTO1.getId());
        assertThat(messageTemplateDTO1).isEqualTo(messageTemplateDTO2);
        messageTemplateDTO2.setId(UUID.randomUUID());
        assertThat(messageTemplateDTO1).isNotEqualTo(messageTemplateDTO2);
        messageTemplateDTO1.setId(null);
        assertThat(messageTemplateDTO1).isNotEqualTo(messageTemplateDTO2);
    }
}
