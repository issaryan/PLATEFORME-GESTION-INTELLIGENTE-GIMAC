package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessReviewDecisionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AccessReviewDecisionDTO.class);
        AccessReviewDecisionDTO accessReviewDecisionDTO1 = new AccessReviewDecisionDTO();
        accessReviewDecisionDTO1.setId(UUID.randomUUID());
        AccessReviewDecisionDTO accessReviewDecisionDTO2 = new AccessReviewDecisionDTO();
        assertThat(accessReviewDecisionDTO1).isNotEqualTo(accessReviewDecisionDTO2);
        accessReviewDecisionDTO2.setId(accessReviewDecisionDTO1.getId());
        assertThat(accessReviewDecisionDTO1).isEqualTo(accessReviewDecisionDTO2);
        accessReviewDecisionDTO2.setId(UUID.randomUUID());
        assertThat(accessReviewDecisionDTO1).isNotEqualTo(accessReviewDecisionDTO2);
        accessReviewDecisionDTO1.setId(null);
        assertThat(accessReviewDecisionDTO1).isNotEqualTo(accessReviewDecisionDTO2);
    }
}
