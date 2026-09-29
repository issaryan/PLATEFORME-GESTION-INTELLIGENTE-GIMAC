package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.AttestationTestSamples.*;
import static cm.gimac.des.certification.domain.CertificationCircuitTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveReportTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class GoLiveDossierTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(GoLiveDossier.class);
        GoLiveDossier goLiveDossier1 = getGoLiveDossierSample1();
        GoLiveDossier goLiveDossier2 = new GoLiveDossier();
        assertThat(goLiveDossier1).isNotEqualTo(goLiveDossier2);

        goLiveDossier2.setId(goLiveDossier1.getId());
        assertThat(goLiveDossier1).isEqualTo(goLiveDossier2);

        goLiveDossier2 = getGoLiveDossierSample2();
        assertThat(goLiveDossier1).isNotEqualTo(goLiveDossier2);
    }

    @Test
    void circuitTest() {
        GoLiveDossier goLiveDossier = getGoLiveDossierRandomSampleGenerator();
        CertificationCircuit certificationCircuitBack = getCertificationCircuitRandomSampleGenerator();

        goLiveDossier.setCircuit(certificationCircuitBack);
        assertThat(goLiveDossier.getCircuit()).isEqualTo(certificationCircuitBack);

        goLiveDossier.circuit(null);
        assertThat(goLiveDossier.getCircuit()).isNull();
    }

    @Test
    void goLiveReportTest() {
        GoLiveDossier goLiveDossier = getGoLiveDossierRandomSampleGenerator();
        GoLiveReport goLiveReportBack = getGoLiveReportRandomSampleGenerator();

        goLiveDossier.setGoLiveReport(goLiveReportBack);
        assertThat(goLiveDossier.getGoLiveReport()).isEqualTo(goLiveReportBack);
        assertThat(goLiveReportBack.getDossier()).isEqualTo(goLiveDossier);

        goLiveDossier.goLiveReport(null);
        assertThat(goLiveDossier.getGoLiveReport()).isNull();
        assertThat(goLiveReportBack.getDossier()).isNull();
    }

    @Test
    void attestationTest() {
        GoLiveDossier goLiveDossier = getGoLiveDossierRandomSampleGenerator();
        Attestation attestationBack = getAttestationRandomSampleGenerator();

        goLiveDossier.setAttestation(attestationBack);
        assertThat(goLiveDossier.getAttestation()).isEqualTo(attestationBack);
        assertThat(attestationBack.getDossier()).isEqualTo(goLiveDossier);

        goLiveDossier.attestation(null);
        assertThat(goLiveDossier.getAttestation()).isNull();
        assertThat(attestationBack.getDossier()).isNull();
    }
}
