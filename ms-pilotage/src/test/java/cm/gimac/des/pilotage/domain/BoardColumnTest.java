package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.BoardColumnTestSamples.*;
import static cm.gimac.des.pilotage.domain.BoardTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BoardColumnTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(BoardColumn.class);
        BoardColumn boardColumn1 = getBoardColumnSample1();
        BoardColumn boardColumn2 = new BoardColumn();
        assertThat(boardColumn1).isNotEqualTo(boardColumn2);

        boardColumn2.setId(boardColumn1.getId());
        assertThat(boardColumn1).isEqualTo(boardColumn2);

        boardColumn2 = getBoardColumnSample2();
        assertThat(boardColumn1).isNotEqualTo(boardColumn2);
    }

    @Test
    void boardTest() {
        BoardColumn boardColumn = getBoardColumnRandomSampleGenerator();
        Board boardBack = getBoardRandomSampleGenerator();

        boardColumn.setBoard(boardBack);
        assertThat(boardColumn.getBoard()).isEqualTo(boardBack);

        boardColumn.board(null);
        assertThat(boardColumn.getBoard()).isNull();
    }
}
