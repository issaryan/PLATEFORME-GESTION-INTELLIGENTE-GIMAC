package cm.gimac.des.collaboration.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoomArchiveDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RoomArchiveDTO.class);
        RoomArchiveDTO roomArchiveDTO1 = new RoomArchiveDTO();
        roomArchiveDTO1.setId(UUID.randomUUID());
        RoomArchiveDTO roomArchiveDTO2 = new RoomArchiveDTO();
        assertThat(roomArchiveDTO1).isNotEqualTo(roomArchiveDTO2);
        roomArchiveDTO2.setId(roomArchiveDTO1.getId());
        assertThat(roomArchiveDTO1).isEqualTo(roomArchiveDTO2);
        roomArchiveDTO2.setId(UUID.randomUUID());
        assertThat(roomArchiveDTO1).isNotEqualTo(roomArchiveDTO2);
        roomArchiveDTO1.setId(null);
        assertThat(roomArchiveDTO1).isNotEqualTo(roomArchiveDTO2);
    }
}
