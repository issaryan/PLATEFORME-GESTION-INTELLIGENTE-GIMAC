package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.ProjectionRuleTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProjectionRuleTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProjectionRule.class);
        ProjectionRule projectionRule1 = getProjectionRuleSample1();
        ProjectionRule projectionRule2 = new ProjectionRule();
        assertThat(projectionRule1).isNotEqualTo(projectionRule2);

        projectionRule2.setId(projectionRule1.getId());
        assertThat(projectionRule1).isEqualTo(projectionRule2);

        projectionRule2 = getProjectionRuleSample2();
        assertThat(projectionRule1).isNotEqualTo(projectionRule2);
    }
}
