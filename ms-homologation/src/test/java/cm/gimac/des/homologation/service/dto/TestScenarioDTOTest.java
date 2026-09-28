package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestScenarioDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TestScenarioDTO.class);
        TestScenarioDTO testScenarioDTO1 = new TestScenarioDTO();
        testScenarioDTO1.setId(UUID.randomUUID());
        TestScenarioDTO testScenarioDTO2 = new TestScenarioDTO();
        assertThat(testScenarioDTO1).isNotEqualTo(testScenarioDTO2);
        testScenarioDTO2.setId(testScenarioDTO1.getId());
        assertThat(testScenarioDTO1).isEqualTo(testScenarioDTO2);
        testScenarioDTO2.setId(UUID.randomUUID());
        assertThat(testScenarioDTO1).isNotEqualTo(testScenarioDTO2);
        testScenarioDTO1.setId(null);
        assertThat(testScenarioDTO1).isNotEqualTo(testScenarioDTO2);
    }
}
