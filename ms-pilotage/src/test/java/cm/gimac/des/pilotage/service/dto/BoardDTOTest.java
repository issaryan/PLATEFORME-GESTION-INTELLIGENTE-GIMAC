package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BoardDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(BoardDTO.class);
        BoardDTO boardDTO1 = new BoardDTO();
        boardDTO1.setId(UUID.randomUUID());
        BoardDTO boardDTO2 = new BoardDTO();
        assertThat(boardDTO1).isNotEqualTo(boardDTO2);
        boardDTO2.setId(boardDTO1.getId());
        assertThat(boardDTO1).isEqualTo(boardDTO2);
        boardDTO2.setId(UUID.randomUUID());
        assertThat(boardDTO1).isNotEqualTo(boardDTO2);
        boardDTO1.setId(null);
        assertThat(boardDTO1).isNotEqualTo(boardDTO2);
    }
}
