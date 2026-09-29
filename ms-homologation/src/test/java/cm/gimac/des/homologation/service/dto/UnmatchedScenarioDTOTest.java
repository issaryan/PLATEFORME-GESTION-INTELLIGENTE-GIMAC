package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UnmatchedScenarioDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(UnmatchedScenarioDTO.class);
        UnmatchedScenarioDTO unmatchedScenarioDTO1 = new UnmatchedScenarioDTO();
        unmatchedScenarioDTO1.setId(UUID.randomUUID());
        UnmatchedScenarioDTO unmatchedScenarioDTO2 = new UnmatchedScenarioDTO();
        assertThat(unmatchedScenarioDTO1).isNotEqualTo(unmatchedScenarioDTO2);
        unmatchedScenarioDTO2.setId(unmatchedScenarioDTO1.getId());
        assertThat(unmatchedScenarioDTO1).isEqualTo(unmatchedScenarioDTO2);
        unmatchedScenarioDTO2.setId(UUID.randomUUID());
        assertThat(unmatchedScenarioDTO1).isNotEqualTo(unmatchedScenarioDTO2);
        unmatchedScenarioDTO1.setId(null);
        assertThat(unmatchedScenarioDTO1).isNotEqualTo(unmatchedScenarioDTO2);
    }
}
