package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.ConsistencyProofTestSamples.*;
import static cm.gimac.des.audit.domain.PeriodRootTestSamples.*;
import static cm.gimac.des.audit.domain.PeriodRootTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PeriodRootTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PeriodRoot.class);
        PeriodRoot periodRoot1 = getPeriodRootSample1();
        PeriodRoot periodRoot2 = new PeriodRoot();
        assertThat(periodRoot1).isNotEqualTo(periodRoot2);

        periodRoot2.setId(periodRoot1.getId());
        assertThat(periodRoot1).isEqualTo(periodRoot2);

        periodRoot2 = getPeriodRootSample2();
        assertThat(periodRoot1).isNotEqualTo(periodRoot2);
    }

    @Test
    void previousRootTest() {
        PeriodRoot periodRoot = getPeriodRootRandomSampleGenerator();
        PeriodRoot periodRootBack = getPeriodRootRandomSampleGenerator();

        periodRoot.setPreviousRoot(periodRootBack);
        assertThat(periodRoot.getPreviousRoot()).isEqualTo(periodRootBack);

        periodRoot.previousRoot(null);
        assertThat(periodRoot.getPreviousRoot()).isNull();
    }

    @Test
    void consistencyWithPreviousTest() {
        PeriodRoot periodRoot = getPeriodRootRandomSampleGenerator();
        ConsistencyProof consistencyProofBack = getConsistencyProofRandomSampleGenerator();

        periodRoot.setConsistencyWithPrevious(consistencyProofBack);
        assertThat(periodRoot.getConsistencyWithPrevious()).isEqualTo(consistencyProofBack);

        periodRoot.consistencyWithPrevious(null);
        assertThat(periodRoot.getConsistencyWithPrevious()).isNull();
    }

    @Test
    void periodRootTest() {
        PeriodRoot periodRoot = getPeriodRootRandomSampleGenerator();
        PeriodRoot periodRootBack = getPeriodRootRandomSampleGenerator();

        periodRoot.setPeriodRoot(periodRootBack);
        assertThat(periodRoot.getPeriodRoot()).isEqualTo(periodRootBack);
        assertThat(periodRootBack.getPreviousRoot()).isEqualTo(periodRoot);

        periodRoot.periodRoot(null);
        assertThat(periodRoot.getPeriodRoot()).isNull();
        assertThat(periodRootBack.getPreviousRoot()).isNull();
    }
}
