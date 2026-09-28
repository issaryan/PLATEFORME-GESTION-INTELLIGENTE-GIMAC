package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.CockpitViewTestSamples.*;
import static cm.gimac.des.reporting.domain.IndicatorDefinitionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IndicatorDefinitionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(IndicatorDefinition.class);
        IndicatorDefinition indicatorDefinition1 = getIndicatorDefinitionSample1();
        IndicatorDefinition indicatorDefinition2 = new IndicatorDefinition();
        assertThat(indicatorDefinition1).isNotEqualTo(indicatorDefinition2);

        indicatorDefinition2.setId(indicatorDefinition1.getId());
        assertThat(indicatorDefinition1).isEqualTo(indicatorDefinition2);

        indicatorDefinition2 = getIndicatorDefinitionSample2();
        assertThat(indicatorDefinition1).isNotEqualTo(indicatorDefinition2);
    }

    @Test
    void cockpitsTest() {
        IndicatorDefinition indicatorDefinition = getIndicatorDefinitionRandomSampleGenerator();
        CockpitView cockpitViewBack = getCockpitViewRandomSampleGenerator();

        indicatorDefinition.addCockpits(cockpitViewBack);
        assertThat(indicatorDefinition.getCockpits()).containsOnly(cockpitViewBack);
        assertThat(cockpitViewBack.getIndicators()).containsOnly(indicatorDefinition);

        indicatorDefinition.removeCockpits(cockpitViewBack);
        assertThat(indicatorDefinition.getCockpits()).doesNotContain(cockpitViewBack);
        assertThat(cockpitViewBack.getIndicators()).doesNotContain(indicatorDefinition);

        indicatorDefinition.cockpits(new HashSet<>(Set.of(cockpitViewBack)));
        assertThat(indicatorDefinition.getCockpits()).containsOnly(cockpitViewBack);
        assertThat(cockpitViewBack.getIndicators()).containsOnly(indicatorDefinition);

        indicatorDefinition.setCockpits(new HashSet<>());
        assertThat(indicatorDefinition.getCockpits()).doesNotContain(cockpitViewBack);
        assertThat(cockpitViewBack.getIndicators()).doesNotContain(indicatorDefinition);
    }
}
