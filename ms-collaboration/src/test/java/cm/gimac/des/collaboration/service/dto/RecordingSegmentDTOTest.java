package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordingSegmentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RecordingSegmentDTO.class);
        RecordingSegmentDTO recordingSegmentDTO1 = new RecordingSegmentDTO();
        recordingSegmentDTO1.setId(UUID.randomUUID());
        RecordingSegmentDTO recordingSegmentDTO2 = new RecordingSegmentDTO();
        assertThat(recordingSegmentDTO1).isNotEqualTo(recordingSegmentDTO2);
        recordingSegmentDTO2.setId(recordingSegmentDTO1.getId());
        assertThat(recordingSegmentDTO1).isEqualTo(recordingSegmentDTO2);
        recordingSegmentDTO2.setId(UUID.randomUUID());
        assertThat(recordingSegmentDTO1).isNotEqualTo(recordingSegmentDTO2);
        recordingSegmentDTO1.setId(null);
        assertThat(recordingSegmentDTO1).isNotEqualTo(recordingSegmentDTO2);
    }
}
