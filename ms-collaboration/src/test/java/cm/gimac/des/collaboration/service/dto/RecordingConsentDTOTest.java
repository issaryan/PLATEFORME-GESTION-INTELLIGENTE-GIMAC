package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordingConsentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingConsentDTO.class);
        RecordingConsentDTO recordingConsentDTO1 = new RecordingConsentDTO();
        recordingConsentDTO1.setId(UUID.randomUUID());
        RecordingConsentDTO recordingConsentDTO2 = new RecordingConsentDTO();
        assertThat(recordingConsentDTO1).isNotEqualTo(recordingConsentDTO2);
        recordingConsentDTO2.setId(recordingConsentDTO1.getId());
        assertThat(recordingConsentDTO1).isEqualTo(recordingConsentDTO2);
        recordingConsentDTO2.setId(UUID.randomUUID());
        assertThat(recordingConsentDTO1).isNotEqualTo(recordingConsentDTO2);
        recordingConsentDTO1.setId(null);
        assertThat(recordingConsentDTO1).isNotEqualTo(recordingConsentDTO2);
    }
}
