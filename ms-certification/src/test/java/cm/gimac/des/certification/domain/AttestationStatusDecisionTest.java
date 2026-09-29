package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.AttestationStatusDecisionTestSamples.*;
import static cm.gimac.des.certification.domain.AttestationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AttestationStatusDecisionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AttestationStatusDecision.class);
        AttestationStatusDecision attestationStatusDecision1 = getAttestationStatusDecisionSample1();
        AttestationStatusDecision attestationStatusDecision2 = new AttestationStatusDecision();
        assertThat(attestationStatusDecision1).isNotEqualTo(attestationStatusDecision2);

        attestationStatusDecision2.setId(attestationStatusDecision1.getId());
        assertThat(attestationStatusDecision1).isEqualTo(attestationStatusDecision2);

        attestationStatusDecision2 = getAttestationStatusDecisionSample2();
        assertThat(attestationStatusDecision1).isNotEqualTo(attestationStatusDecision2);
    }

    @Test
    void attestationTest() {
        AttestationStatusDecision attestationStatusDecision = getAttestationStatusDecisionRandomSampleGenerator();
        Attestation attestationBack = getAttestationRandomSampleGenerator();

        attestationStatusDecision.setAttestation(attestationBack);
        assertThat(attestationStatusDecision.getAttestation()).isEqualTo(attestationBack);

        attestationStatusDecision.attestation(null);
        assertThat(attestationStatusDecision.getAttestation()).isNull();
    }
}
