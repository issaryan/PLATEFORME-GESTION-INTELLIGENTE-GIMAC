package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.MeasureWindowTestSamples.*;
import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class MeasureWindowTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MeasureWindow.class);
        MeasureWindow measureWindow1 = getMeasureWindowSample1();
        MeasureWindow measureWindow2 = new MeasureWindow();
        assertThat(measureWindow1).isNotEqualTo(measureWindow2);

        measureWindow2.setId(measureWindow1.getId());
        assertThat(measureWindow1).isEqualTo(measureWindow2);

        measureWindow2 = getMeasureWindowSample2();
        assertThat(measureWindow1).isNotEqualTo(measureWindow2);
    }

    @Test
    void linkTest() {
        MeasureWindow measureWindow = getMeasureWindowRandomSampleGenerator();
        SupervisedLink supervisedLinkBack = getSupervisedLinkRandomSampleGenerator();

        measureWindow.setLink(supervisedLinkBack);
        assertThat(measureWindow.getLink()).isEqualTo(supervisedLinkBack);

        measureWindow.link(null);
        assertThat(measureWindow.getLink()).isNull();
    }
}
