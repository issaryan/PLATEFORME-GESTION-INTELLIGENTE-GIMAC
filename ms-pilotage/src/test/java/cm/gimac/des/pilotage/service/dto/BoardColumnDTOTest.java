package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BoardColumnDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(BoardColumnDTO.class);
        BoardColumnDTO boardColumnDTO1 = new BoardColumnDTO();
        boardColumnDTO1.setId(UUID.randomUUID());
        BoardColumnDTO boardColumnDTO2 = new BoardColumnDTO();
        assertThat(boardColumnDTO1).isNotEqualTo(boardColumnDTO2);
        boardColumnDTO2.setId(boardColumnDTO1.getId());
        assertThat(boardColumnDTO1).isEqualTo(boardColumnDTO2);
        boardColumnDTO2.setId(UUID.randomUUID());
        assertThat(boardColumnDTO1).isNotEqualTo(boardColumnDTO2);
        boardColumnDTO1.setId(null);
        assertThat(boardColumnDTO1).isNotEqualTo(boardColumnDTO2);
    }
}
