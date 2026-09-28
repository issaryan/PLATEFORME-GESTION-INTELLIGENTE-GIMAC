package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.ActionSheetTestSamples.*;
import static cm.gimac.des.pilotage.domain.BoardColumnTestSamples.*;
import static cm.gimac.des.pilotage.domain.BoardTestSamples.*;
import static cm.gimac.des.pilotage.domain.DecisionTestSamples.*;
import static cm.gimac.des.pilotage.domain.TaskTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TaskTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Task.class);
        Task task1 = getTaskSample1();
        Task task2 = new Task();
        assertThat(task1).isNotEqualTo(task2);

        task2.setId(task1.getId());
        assertThat(task1).isEqualTo(task2);

        task2 = getTaskSample2();
        assertThat(task1).isNotEqualTo(task2);
    }

    @Test
    void boardTest() {
        Task task = getTaskRandomSampleGenerator();
        Board boardBack = getBoardRandomSampleGenerator();

        task.setBoard(boardBack);
        assertThat(task.getBoard()).isEqualTo(boardBack);

        task.board(null);
        assertThat(task.getBoard()).isNull();
    }

    @Test
    void boardColumnTest() {
        Task task = getTaskRandomSampleGenerator();
        BoardColumn boardColumnBack = getBoardColumnRandomSampleGenerator();

        task.setBoardColumn(boardColumnBack);
        assertThat(task.getBoardColumn()).isEqualTo(boardColumnBack);

        task.boardColumn(null);
        assertThat(task.getBoardColumn()).isNull();
    }

    @Test
    void actionSheetTest() {
        Task task = getTaskRandomSampleGenerator();
        ActionSheet actionSheetBack = getActionSheetRandomSampleGenerator();

        task.setActionSheet(actionSheetBack);
        assertThat(task.getActionSheet()).isEqualTo(actionSheetBack);

        task.actionSheet(null);
        assertThat(task.getActionSheet()).isNull();
    }

    @Test
    void decisionTest() {
        Task task = getTaskRandomSampleGenerator();
        Decision decisionBack = getDecisionRandomSampleGenerator();

        task.setDecision(decisionBack);
        assertThat(task.getDecision()).isEqualTo(decisionBack);
        assertThat(decisionBack.getTask()).isEqualTo(task);

        task.decision(null);
        assertThat(task.getDecision()).isNull();
        assertThat(decisionBack.getTask()).isNull();
    }
}
