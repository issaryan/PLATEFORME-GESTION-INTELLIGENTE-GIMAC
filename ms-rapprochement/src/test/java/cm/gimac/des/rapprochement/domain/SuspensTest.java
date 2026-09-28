package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.ReconciliationTestSamples.*;
import static cm.gimac.des.rapprochement.domain.RegularizationAttestationTestSamples.*;
import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SuspensTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Suspens.class);
        Suspens suspens1 = getSuspensSample1();
        Suspens suspens2 = new Suspens();
        assertThat(suspens1).isNotEqualTo(suspens2);

        suspens2.setId(suspens1.getId());
        assertThat(suspens1).isEqualTo(suspens2);

        suspens2 = getSuspensSample2();
        assertThat(suspens1).isNotEqualTo(suspens2);
    }

    @Test
    void reconciliationTest() {
        Suspens suspens = getSuspensRandomSampleGenerator();
        Reconciliation reconciliationBack = getReconciliationRandomSampleGenerator();

        suspens.setReconciliation(reconciliationBack);
        assertThat(suspens.getReconciliation()).isEqualTo(reconciliationBack);

        suspens.reconciliation(null);
        assertThat(suspens.getReconciliation()).isNull();
    }

    @Test
    void regularizationAttestationTest() {
        Suspens suspens = getSuspensRandomSampleGenerator();
        RegularizationAttestation regularizationAttestationBack = getRegularizationAttestationRandomSampleGenerator();

        suspens.setRegularizationAttestation(regularizationAttestationBack);
        assertThat(suspens.getRegularizationAttestation()).isEqualTo(regularizationAttestationBack);
        assertThat(regularizationAttestationBack.getSuspens()).isEqualTo(suspens);

        suspens.regularizationAttestation(null);
        assertThat(suspens.getRegularizationAttestation()).isNull();
        assertThat(regularizationAttestationBack.getSuspens()).isNull();
    }
}
