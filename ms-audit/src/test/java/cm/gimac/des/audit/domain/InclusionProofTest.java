package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.InclusionProofTestSamples.*;
import static cm.gimac.des.audit.domain.PeriodRootTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class InclusionProofTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(InclusionProof.class);
        InclusionProof inclusionProof1 = getInclusionProofSample1();
        InclusionProof inclusionProof2 = new InclusionProof();
        assertThat(inclusionProof1).isNotEqualTo(inclusionProof2);

        inclusionProof2.setId(inclusionProof1.getId());
        assertThat(inclusionProof1).isEqualTo(inclusionProof2);

        inclusionProof2 = getInclusionProofSample2();
        assertThat(inclusionProof1).isNotEqualTo(inclusionProof2);
    }

    @Test
    void periodRootTest() {
        InclusionProof inclusionProof = getInclusionProofRandomSampleGenerator();
        PeriodRoot periodRootBack = getPeriodRootRandomSampleGenerator();

        inclusionProof.setPeriodRoot(periodRootBack);
        assertThat(inclusionProof.getPeriodRoot()).isEqualTo(periodRootBack);

        inclusionProof.periodRoot(null);
        assertThat(inclusionProof.getPeriodRoot()).isNull();
    }
}
