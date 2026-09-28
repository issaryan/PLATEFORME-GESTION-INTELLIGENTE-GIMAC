package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;
import static cm.gimac.des.certification.domain.QuitusTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class QuitusTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Quitus.class);
        Quitus quitus1 = getQuitusSample1();
        Quitus quitus2 = new Quitus();
        assertThat(quitus1).isNotEqualTo(quitus2);

        quitus2.setId(quitus1.getId());
        assertThat(quitus1).isEqualTo(quitus2);

        quitus2 = getQuitusSample2();
        assertThat(quitus1).isNotEqualTo(quitus2);
    }

    @Test
    void fileTest() {
        Quitus quitus = getQuitusRandomSampleGenerator();
        CommissioningFile commissioningFileBack = getCommissioningFileRandomSampleGenerator();

        quitus.setFile(commissioningFileBack);
        assertThat(quitus.getFile()).isEqualTo(commissioningFileBack);

        quitus.file(null);
        assertThat(quitus.getFile()).isNull();
    }
}
