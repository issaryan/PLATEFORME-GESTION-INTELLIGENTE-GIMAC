package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.MessageMetadataTestSamples.*;
import static cm.gimac.des.collaboration.domain.MessageReceiptTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MessageReceiptTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageReceipt.class);
        MessageReceipt messageReceipt1 = getMessageReceiptSample1();
        MessageReceipt messageReceipt2 = new MessageReceipt();
        assertThat(messageReceipt1).isNotEqualTo(messageReceipt2);

        messageReceipt2.setId(messageReceipt1.getId());
        assertThat(messageReceipt1).isEqualTo(messageReceipt2);

        messageReceipt2 = getMessageReceiptSample2();
        assertThat(messageReceipt1).isNotEqualTo(messageReceipt2);
    }

    @Test
    void messageTest() {
        MessageReceipt messageReceipt = getMessageReceiptRandomSampleGenerator();
        MessageMetadata messageMetadataBack = getMessageMetadataRandomSampleGenerator();

        messageReceipt.setMessage(messageMetadataBack);
        assertThat(messageReceipt.getMessage()).isEqualTo(messageMetadataBack);

        messageReceipt.message(null);
        assertThat(messageReceipt.getMessage()).isNull();
    }
}
