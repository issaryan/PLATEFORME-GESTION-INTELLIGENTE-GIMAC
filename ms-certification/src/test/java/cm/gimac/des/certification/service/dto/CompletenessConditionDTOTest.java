package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompletenessConditionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CompletenessConditionDTO.class);
        CompletenessConditionDTO completenessConditionDTO1 = new CompletenessConditionDTO();
        completenessConditionDTO1.setId(UUID.randomUUID());
        CompletenessConditionDTO completenessConditionDTO2 = new CompletenessConditionDTO();
        assertThat(completenessConditionDTO1).isNotEqualTo(completenessConditionDTO2);
        completenessConditionDTO2.setId(completenessConditionDTO1.getId());
        assertThat(completenessConditionDTO1).isEqualTo(completenessConditionDTO2);
        completenessConditionDTO2.setId(UUID.randomUUID());
        assertThat(completenessConditionDTO1).isNotEqualTo(completenessConditionDTO2);
        completenessConditionDTO1.setId(null);
        assertThat(completenessConditionDTO1).isNotEqualTo(completenessConditionDTO2);
    }
}
