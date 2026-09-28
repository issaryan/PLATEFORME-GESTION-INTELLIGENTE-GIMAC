package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.StatusTransitionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StatusTransitionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StatusTransition.class);
        StatusTransition statusTransition1 = getStatusTransitionSample1();
        StatusTransition statusTransition2 = new StatusTransition();
        assertThat(statusTransition1).isNotEqualTo(statusTransition2);

        statusTransition2.setId(statusTransition1.getId());
        assertThat(statusTransition1).isEqualTo(statusTransition2);

        statusTransition2 = getStatusTransitionSample2();
        assertThat(statusTransition1).isNotEqualTo(statusTransition2);
    }

    @Test
    void institutionTest() {
        StatusTransition statusTransition = getStatusTransitionRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        statusTransition.setInstitution(institutionBack);
        assertThat(statusTransition.getInstitution()).isEqualTo(institutionBack);

        statusTransition.institution(null);
        assertThat(statusTransition.getInstitution()).isNull();
    }
}
