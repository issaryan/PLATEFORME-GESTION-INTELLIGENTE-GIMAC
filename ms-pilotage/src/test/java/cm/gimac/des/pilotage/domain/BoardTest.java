package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.BoardTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BoardTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Board.class);
        Board board1 = getBoardSample1();
        Board board2 = new Board();
        assertThat(board1).isNotEqualTo(board2);

        board2.setId(board1.getId());
        assertThat(board1).isEqualTo(board2);

        board2 = getBoardSample2();
        assertThat(board1).isNotEqualTo(board2);
    }
}
