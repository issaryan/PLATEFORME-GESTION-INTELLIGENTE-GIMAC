package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageReceiptDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageReceiptDTO.class);
        MessageReceiptDTO messageReceiptDTO1 = new MessageReceiptDTO();
        messageReceiptDTO1.setId(UUID.randomUUID());
        MessageReceiptDTO messageReceiptDTO2 = new MessageReceiptDTO();
        assertThat(messageReceiptDTO1).isNotEqualTo(messageReceiptDTO2);
        messageReceiptDTO2.setId(messageReceiptDTO1.getId());
        assertThat(messageReceiptDTO1).isEqualTo(messageReceiptDTO2);
        messageReceiptDTO2.setId(UUID.randomUUID());
        assertThat(messageReceiptDTO1).isNotEqualTo(messageReceiptDTO2);
        messageReceiptDTO1.setId(null);
        assertThat(messageReceiptDTO1).isNotEqualTo(messageReceiptDTO2);
    }
}
