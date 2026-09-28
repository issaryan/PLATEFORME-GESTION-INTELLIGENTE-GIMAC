package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;
import static cm.gimac.des.monitoring.domain.ThresholdTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ThresholdTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Threshold.class);
        Threshold threshold1 = getThresholdSample1();
        Threshold threshold2 = new Threshold();
        assertThat(threshold1).isNotEqualTo(threshold2);

        threshold2.setId(threshold1.getId());
        assertThat(threshold1).isEqualTo(threshold2);

        threshold2 = getThresholdSample2();
        assertThat(threshold1).isNotEqualTo(threshold2);
    }

    @Test
    void linkTest() {
        Threshold threshold = getThresholdRandomSampleGenerator();
        SupervisedLink supervisedLinkBack = getSupervisedLinkRandomSampleGenerator();

        threshold.setLink(supervisedLinkBack);
        assertThat(threshold.getLink()).isEqualTo(supervisedLinkBack);

        threshold.link(null);
        assertThat(threshold.getLink()).isNull();
    }
}
