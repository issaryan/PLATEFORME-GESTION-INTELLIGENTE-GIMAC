package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.ConsistencyProofTestSamples.*;
import static cm.gimac.des.audit.domain.PeriodRootTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConsistencyProofTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConsistencyProof.class);
        ConsistencyProof consistencyProof1 = getConsistencyProofSample1();
        ConsistencyProof consistencyProof2 = new ConsistencyProof();
        assertThat(consistencyProof1).isNotEqualTo(consistencyProof2);

        consistencyProof2.setId(consistencyProof1.getId());
        assertThat(consistencyProof1).isEqualTo(consistencyProof2);

        consistencyProof2 = getConsistencyProofSample2();
        assertThat(consistencyProof1).isNotEqualTo(consistencyProof2);
    }

    @Test
    void periodRootTest() {
        ConsistencyProof consistencyProof = getConsistencyProofRandomSampleGenerator();
        PeriodRoot periodRootBack = getPeriodRootRandomSampleGenerator();

        consistencyProof.setPeriodRoot(periodRootBack);
        assertThat(consistencyProof.getPeriodRoot()).isEqualTo(periodRootBack);
        assertThat(periodRootBack.getConsistencyWithPrevious()).isEqualTo(consistencyProof);

        consistencyProof.periodRoot(null);
        assertThat(consistencyProof.getPeriodRoot()).isNull();
        assertThat(periodRootBack.getConsistencyWithPrevious()).isNull();
    }
}
