package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.CockpitViewTestSamples.*;
import static cm.gimac.des.reporting.domain.IndicatorDefinitionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CockpitViewTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CockpitView.class);
        CockpitView cockpitView1 = getCockpitViewSample1();
        CockpitView cockpitView2 = new CockpitView();
        assertThat(cockpitView1).isNotEqualTo(cockpitView2);

        cockpitView2.setId(cockpitView1.getId());
        assertThat(cockpitView1).isEqualTo(cockpitView2);

        cockpitView2 = getCockpitViewSample2();
        assertThat(cockpitView1).isNotEqualTo(cockpitView2);
    }

    @Test
    void indicatorsTest() {
        CockpitView cockpitView = getCockpitViewRandomSampleGenerator();
        IndicatorDefinition indicatorDefinitionBack = getIndicatorDefinitionRandomSampleGenerator();

        cockpitView.addIndicators(indicatorDefinitionBack);
        assertThat(cockpitView.getIndicators()).containsOnly(indicatorDefinitionBack);

        cockpitView.removeIndicators(indicatorDefinitionBack);
        assertThat(cockpitView.getIndicators()).doesNotContain(indicatorDefinitionBack);

        cockpitView.indicators(new HashSet<>(Set.of(indicatorDefinitionBack)));
        assertThat(cockpitView.getIndicators()).containsOnly(indicatorDefinitionBack);

        cockpitView.setIndicators(new HashSet<>());
        assertThat(cockpitView.getIndicators()).doesNotContain(indicatorDefinitionBack);
    }
}
