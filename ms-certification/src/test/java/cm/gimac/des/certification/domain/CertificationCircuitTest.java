package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CertificationCircuitTestSamples.*;
import static cm.gimac.des.certification.domain.GoLiveDossierTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CertificationCircuitTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationCircuit.class);
        CertificationCircuit certificationCircuit1 = getCertificationCircuitSample1();
        CertificationCircuit certificationCircuit2 = new CertificationCircuit();
        assertThat(certificationCircuit1).isNotEqualTo(certificationCircuit2);

        certificationCircuit2.setId(certificationCircuit1.getId());
        assertThat(certificationCircuit1).isEqualTo(certificationCircuit2);

        certificationCircuit2 = getCertificationCircuitSample2();
        assertThat(certificationCircuit1).isNotEqualTo(certificationCircuit2);
    }

    @Test
    void goLiveDossierTest() {
        CertificationCircuit certificationCircuit = getCertificationCircuitRandomSampleGenerator();
        GoLiveDossier goLiveDossierBack = getGoLiveDossierRandomSampleGenerator();

        certificationCircuit.setGoLiveDossier(goLiveDossierBack);
        assertThat(certificationCircuit.getGoLiveDossier()).isEqualTo(goLiveDossierBack);
        assertThat(goLiveDossierBack.getCircuit()).isEqualTo(certificationCircuit);

        certificationCircuit.goLiveDossier(null);
        assertThat(certificationCircuit.getGoLiveDossier()).isNull();
        assertThat(goLiveDossierBack.getCircuit()).isNull();
    }
}
