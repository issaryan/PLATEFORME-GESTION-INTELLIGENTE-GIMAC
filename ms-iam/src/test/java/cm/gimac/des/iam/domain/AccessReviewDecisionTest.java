package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.AccessReviewDecisionTestSamples.*;
import static cm.gimac.des.iam.domain.AccessReviewTestSamples.*;
import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AccessReviewDecisionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AccessReviewDecision.class);
        AccessReviewDecision accessReviewDecision1 = getAccessReviewDecisionSample1();
        AccessReviewDecision accessReviewDecision2 = new AccessReviewDecision();
        assertThat(accessReviewDecision1).isNotEqualTo(accessReviewDecision2);

        accessReviewDecision2.setId(accessReviewDecision1.getId());
        assertThat(accessReviewDecision1).isEqualTo(accessReviewDecision2);

        accessReviewDecision2 = getAccessReviewDecisionSample2();
        assertThat(accessReviewDecision1).isNotEqualTo(accessReviewDecision2);
    }

    @Test
    void reviewTest() {
        AccessReviewDecision accessReviewDecision = getAccessReviewDecisionRandomSampleGenerator();
        AccessReview accessReviewBack = getAccessReviewRandomSampleGenerator();

        accessReviewDecision.setReview(accessReviewBack);
        assertThat(accessReviewDecision.getReview()).isEqualTo(accessReviewBack);

        accessReviewDecision.review(null);
        assertThat(accessReviewDecision.getReview()).isNull();
    }

    @Test
    void collaboratorTest() {
        AccessReviewDecision accessReviewDecision = getAccessReviewDecisionRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        accessReviewDecision.setCollaborator(collaboratorBack);
        assertThat(accessReviewDecision.getCollaborator()).isEqualTo(collaboratorBack);

        accessReviewDecision.collaborator(null);
        assertThat(accessReviewDecision.getCollaborator()).isNull();
    }
}
