package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationBaseDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvaluationBaseDTO.class);
        EvaluationBaseDTO evaluationBaseDTO1 = new EvaluationBaseDTO();
        evaluationBaseDTO1.setId(UUID.randomUUID());
        EvaluationBaseDTO evaluationBaseDTO2 = new EvaluationBaseDTO();
        assertThat(evaluationBaseDTO1).isNotEqualTo(evaluationBaseDTO2);
        evaluationBaseDTO2.setId(evaluationBaseDTO1.getId());
        assertThat(evaluationBaseDTO1).isEqualTo(evaluationBaseDTO2);
        evaluationBaseDTO2.setId(UUID.randomUUID());
        assertThat(evaluationBaseDTO1).isNotEqualTo(evaluationBaseDTO2);
        evaluationBaseDTO1.setId(null);
        assertThat(evaluationBaseDTO1).isNotEqualTo(evaluationBaseDTO2);
    }
}
