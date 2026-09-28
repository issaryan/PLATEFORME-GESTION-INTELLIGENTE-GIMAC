package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaKeyEpochDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(MediaKeyEpochDTO.class);
        MediaKeyEpochDTO mediaKeyEpochDTO1 = new MediaKeyEpochDTO();
        mediaKeyEpochDTO1.setId(UUID.randomUUID());
        MediaKeyEpochDTO mediaKeyEpochDTO2 = new MediaKeyEpochDTO();
        assertThat(mediaKeyEpochDTO1).isNotEqualTo(mediaKeyEpochDTO2);
        mediaKeyEpochDTO2.setId(mediaKeyEpochDTO1.getId());
        assertThat(mediaKeyEpochDTO1).isEqualTo(mediaKeyEpochDTO2);
        mediaKeyEpochDTO2.setId(UUID.randomUUID());
        assertThat(mediaKeyEpochDTO1).isNotEqualTo(mediaKeyEpochDTO2);
        mediaKeyEpochDTO1.setId(null);
        assertThat(mediaKeyEpochDTO1).isNotEqualTo(mediaKeyEpochDTO2);
    }
}
