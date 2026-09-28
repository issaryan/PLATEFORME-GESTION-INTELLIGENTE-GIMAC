package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcceptanceCriterionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AcceptanceCriterionDTO.class);
        AcceptanceCriterionDTO acceptanceCriterionDTO1 = new AcceptanceCriterionDTO();
        acceptanceCriterionDTO1.setId(UUID.randomUUID());
        AcceptanceCriterionDTO acceptanceCriterionDTO2 = new AcceptanceCriterionDTO();
        assertThat(acceptanceCriterionDTO1).isNotEqualTo(acceptanceCriterionDTO2);
        acceptanceCriterionDTO2.setId(acceptanceCriterionDTO1.getId());
        assertThat(acceptanceCriterionDTO1).isEqualTo(acceptanceCriterionDTO2);
        acceptanceCriterionDTO2.setId(UUID.randomUUID());
        assertThat(acceptanceCriterionDTO1).isNotEqualTo(acceptanceCriterionDTO2);
        acceptanceCriterionDTO1.setId(null);
        assertThat(acceptanceCriterionDTO1).isNotEqualTo(acceptanceCriterionDTO2);
    }
}
