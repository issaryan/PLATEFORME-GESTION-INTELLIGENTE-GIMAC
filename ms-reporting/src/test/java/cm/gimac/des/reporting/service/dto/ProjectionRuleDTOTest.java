package cm.gimac.des.reporting.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectionRuleDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProjectionRuleDTO.class);
        ProjectionRuleDTO projectionRuleDTO1 = new ProjectionRuleDTO();
        projectionRuleDTO1.setId(UUID.randomUUID());
        ProjectionRuleDTO projectionRuleDTO2 = new ProjectionRuleDTO();
        assertThat(projectionRuleDTO1).isNotEqualTo(projectionRuleDTO2);
        projectionRuleDTO2.setId(projectionRuleDTO1.getId());
        assertThat(projectionRuleDTO1).isEqualTo(projectionRuleDTO2);
        projectionRuleDTO2.setId(UUID.randomUUID());
        assertThat(projectionRuleDTO1).isNotEqualTo(projectionRuleDTO2);
        projectionRuleDTO1.setId(null);
        assertThat(projectionRuleDTO1).isNotEqualTo(projectionRuleDTO2);
    }
}
