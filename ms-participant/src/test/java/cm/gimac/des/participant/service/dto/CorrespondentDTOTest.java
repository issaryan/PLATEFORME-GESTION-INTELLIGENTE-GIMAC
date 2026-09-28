package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrespondentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CorrespondentDTO.class);
        CorrespondentDTO correspondentDTO1 = new CorrespondentDTO();
        correspondentDTO1.setId(UUID.randomUUID());
        CorrespondentDTO correspondentDTO2 = new CorrespondentDTO();
        assertThat(correspondentDTO1).isNotEqualTo(correspondentDTO2);
        correspondentDTO2.setId(correspondentDTO1.getId());
        assertThat(correspondentDTO1).isEqualTo(correspondentDTO2);
        correspondentDTO2.setId(UUID.randomUUID());
        assertThat(correspondentDTO1).isNotEqualTo(correspondentDTO2);
        correspondentDTO1.setId(null);
        assertThat(correspondentDTO1).isNotEqualTo(correspondentDTO2);
    }
}
