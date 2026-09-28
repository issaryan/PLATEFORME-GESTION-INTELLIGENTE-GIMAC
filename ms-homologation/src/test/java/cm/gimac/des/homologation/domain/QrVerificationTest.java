package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static cm.gimac.des.homologation.domain.OperationTestSamples.*;
import static cm.gimac.des.homologation.domain.QrVerificationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class QrVerificationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(QrVerification.class);
        QrVerification qrVerification1 = getQrVerificationSample1();
        QrVerification qrVerification2 = new QrVerification();
        assertThat(qrVerification1).isNotEqualTo(qrVerification2);

        qrVerification2.setId(qrVerification1.getId());
        assertThat(qrVerification1).isEqualTo(qrVerification2);

        qrVerification2 = getQrVerificationSample2();
        assertThat(qrVerification1).isNotEqualTo(qrVerification2);
    }

    @Test
    void liaisonTest() {
        QrVerification qrVerification = getQrVerificationRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        qrVerification.setLiaison(liaisonBack);
        assertThat(qrVerification.getLiaison()).isEqualTo(liaisonBack);

        qrVerification.liaison(null);
        assertThat(qrVerification.getLiaison()).isNull();
    }

    @Test
    void operationTest() {
        QrVerification qrVerification = getQrVerificationRandomSampleGenerator();
        Operation operationBack = getOperationRandomSampleGenerator();

        qrVerification.setOperation(operationBack);
        assertThat(qrVerification.getOperation()).isEqualTo(operationBack);

        qrVerification.operation(null);
        assertThat(qrVerification.getOperation()).isNull();
    }
}
