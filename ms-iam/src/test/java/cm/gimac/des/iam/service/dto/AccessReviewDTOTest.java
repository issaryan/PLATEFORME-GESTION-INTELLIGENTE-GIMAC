package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessReviewDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AccessReviewDTO.class);
        AccessReviewDTO accessReviewDTO1 = new AccessReviewDTO();
        accessReviewDTO1.setId(UUID.randomUUID());
        AccessReviewDTO accessReviewDTO2 = new AccessReviewDTO();
        assertThat(accessReviewDTO1).isNotEqualTo(accessReviewDTO2);
        accessReviewDTO2.setId(accessReviewDTO1.getId());
        assertThat(accessReviewDTO1).isEqualTo(accessReviewDTO2);
        accessReviewDTO2.setId(UUID.randomUUID());
        assertThat(accessReviewDTO1).isNotEqualTo(accessReviewDTO2);
        accessReviewDTO1.setId(null);
        assertThat(accessReviewDTO1).isNotEqualTo(accessReviewDTO2);
    }
}
