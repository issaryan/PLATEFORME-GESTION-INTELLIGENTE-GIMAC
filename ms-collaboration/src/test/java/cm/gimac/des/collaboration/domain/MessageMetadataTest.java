package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.MessageMetadataTestSamples.*;
import static cm.gimac.des.collaboration.domain.RoomTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MessageMetadataTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageMetadata.class);
        MessageMetadata messageMetadata1 = getMessageMetadataSample1();
        MessageMetadata messageMetadata2 = new MessageMetadata();
        assertThat(messageMetadata1).isNotEqualTo(messageMetadata2);

        messageMetadata2.setId(messageMetadata1.getId());
        assertThat(messageMetadata1).isEqualTo(messageMetadata2);

        messageMetadata2 = getMessageMetadataSample2();
        assertThat(messageMetadata1).isNotEqualTo(messageMetadata2);
    }

    @Test
    void roomTest() {
        MessageMetadata messageMetadata = getMessageMetadataRandomSampleGenerator();
        Room roomBack = getRoomRandomSampleGenerator();

        messageMetadata.setRoom(roomBack);
        assertThat(messageMetadata.getRoom()).isEqualTo(roomBack);

        messageMetadata.room(null);
        assertThat(messageMetadata.getRoom()).isNull();
    }
}
