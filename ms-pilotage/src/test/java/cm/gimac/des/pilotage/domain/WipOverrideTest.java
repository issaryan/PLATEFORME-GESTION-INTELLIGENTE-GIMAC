package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.BoardColumnTestSamples.*;
import static cm.gimac.des.pilotage.domain.TaskTestSamples.*;
import static cm.gimac.des.pilotage.domain.WipOverrideTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class WipOverrideTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(WipOverride.class);
        WipOverride wipOverride1 = getWipOverrideSample1();
        WipOverride wipOverride2 = new WipOverride();
        assertThat(wipOverride1).isNotEqualTo(wipOverride2);

        wipOverride2.setId(wipOverride1.getId());
        assertThat(wipOverride1).isEqualTo(wipOverride2);

        wipOverride2 = getWipOverrideSample2();
        assertThat(wipOverride1).isNotEqualTo(wipOverride2);
    }

    @Test
    void boardColumnTest() {
        WipOverride wipOverride = getWipOverrideRandomSampleGenerator();
        BoardColumn boardColumnBack = getBoardColumnRandomSampleGenerator();

        wipOverride.setBoardColumn(boardColumnBack);
        assertThat(wipOverride.getBoardColumn()).isEqualTo(boardColumnBack);

        wipOverride.boardColumn(null);
        assertThat(wipOverride.getBoardColumn()).isNull();
    }

    @Test
    void taskTest() {
        WipOverride wipOverride = getWipOverrideRandomSampleGenerator();
        Task taskBack = getTaskRandomSampleGenerator();

        wipOverride.setTask(taskBack);
        assertThat(wipOverride.getTask()).isEqualTo(taskBack);

        wipOverride.task(null);
        assertThat(wipOverride.getTask()).isNull();
    }
}
