package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.AccessReviewTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AccessReviewTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AccessReview.class);
        AccessReview accessReview1 = getAccessReviewSample1();
        AccessReview accessReview2 = new AccessReview();
        assertThat(accessReview1).isNotEqualTo(accessReview2);

        accessReview2.setId(accessReview1.getId());
        assertThat(accessReview1).isEqualTo(accessReview2);

        accessReview2 = getAccessReviewSample2();
        assertThat(accessReview1).isNotEqualTo(accessReview2);
    }
}
