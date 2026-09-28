package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.RegularizationAttestationTestSamples.*;
import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RegularizationAttestationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RegularizationAttestation.class);
        RegularizationAttestation regularizationAttestation1 = getRegularizationAttestationSample1();
        RegularizationAttestation regularizationAttestation2 = new RegularizationAttestation();
        assertThat(regularizationAttestation1).isNotEqualTo(regularizationAttestation2);

        regularizationAttestation2.setId(regularizationAttestation1.getId());
        assertThat(regularizationAttestation1).isEqualTo(regularizationAttestation2);

        regularizationAttestation2 = getRegularizationAttestationSample2();
        assertThat(regularizationAttestation1).isNotEqualTo(regularizationAttestation2);
    }

    @Test
    void suspensTest() {
        RegularizationAttestation regularizationAttestation = getRegularizationAttestationRandomSampleGenerator();
        Suspens suspensBack = getSuspensRandomSampleGenerator();

        regularizationAttestation.setSuspens(suspensBack);
        assertThat(regularizationAttestation.getSuspens()).isEqualTo(suspensBack);

        regularizationAttestation.suspens(null);
        assertThat(regularizationAttestation.getSuspens()).isNull();
    }
}
