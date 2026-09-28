package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageMetadataDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MessageMetadataDTO.class);
        MessageMetadataDTO messageMetadataDTO1 = new MessageMetadataDTO();
        messageMetadataDTO1.setId(UUID.randomUUID());
        MessageMetadataDTO messageMetadataDTO2 = new MessageMetadataDTO();
        assertThat(messageMetadataDTO1).isNotEqualTo(messageMetadataDTO2);
        messageMetadataDTO2.setId(messageMetadataDTO1.getId());
        assertThat(messageMetadataDTO1).isEqualTo(messageMetadataDTO2);
        messageMetadataDTO2.setId(UUID.randomUUID());
        assertThat(messageMetadataDTO1).isNotEqualTo(messageMetadataDTO2);
        messageMetadataDTO1.setId(null);
        assertThat(messageMetadataDTO1).isNotEqualTo(messageMetadataDTO2);
    }
}
