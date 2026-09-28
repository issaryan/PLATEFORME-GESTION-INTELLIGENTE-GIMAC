package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CertificationAttestationTestSamples.*;
import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CertificationAttestationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationAttestation.class);
        CertificationAttestation certificationAttestation1 = getCertificationAttestationSample1();
        CertificationAttestation certificationAttestation2 = new CertificationAttestation();
        assertThat(certificationAttestation1).isNotEqualTo(certificationAttestation2);

        certificationAttestation2.setId(certificationAttestation1.getId());
        assertThat(certificationAttestation1).isEqualTo(certificationAttestation2);

        certificationAttestation2 = getCertificationAttestationSample2();
        assertThat(certificationAttestation1).isNotEqualTo(certificationAttestation2);
    }

    @Test
    void commissioningFileTest() {
        CertificationAttestation certificationAttestation = getCertificationAttestationRandomSampleGenerator();
        CommissioningFile commissioningFileBack = getCommissioningFileRandomSampleGenerator();

        certificationAttestation.setCommissioningFile(commissioningFileBack);
        assertThat(certificationAttestation.getCommissioningFile()).isEqualTo(commissioningFileBack);
        assertThat(commissioningFileBack.getAttestation()).isEqualTo(certificationAttestation);

        certificationAttestation.commissioningFile(null);
        assertThat(certificationAttestation.getCommissioningFile()).isNull();
        assertThat(commissioningFileBack.getAttestation()).isNull();
    }
}
