package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.AttestationTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AttestationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Attestation.class);
        Attestation attestation1 = getAttestationSample1();
        Attestation attestation2 = new Attestation();
        assertThat(attestation1).isNotEqualTo(attestation2);

        attestation2.setId(attestation1.getId());
        assertThat(attestation1).isEqualTo(attestation2);

        attestation2 = getAttestationSample2();
        assertThat(attestation1).isNotEqualTo(attestation2);
    }

    @Test
    void dossierTest() {
        Attestation attestation = getAttestationRandomSampleGenerator();
        GoLiveDossier goLiveDossierBack = getGoLiveDossierRandomSampleGenerator();

        attestation.setDossier(goLiveDossierBack);
        assertThat(attestation.getDossier()).isEqualTo(goLiveDossierBack);

        attestation.dossier(null);
        assertThat(attestation.getDossier()).isNull();
    }
}
