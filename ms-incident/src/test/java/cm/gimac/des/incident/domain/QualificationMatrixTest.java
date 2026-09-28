package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.QualificationMatrixTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class QualificationMatrixTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(QualificationMatrix.class);
        QualificationMatrix qualificationMatrix1 = getQualificationMatrixSample1();
        QualificationMatrix qualificationMatrix2 = new QualificationMatrix();
        assertThat(qualificationMatrix1).isNotEqualTo(qualificationMatrix2);

        qualificationMatrix2.setId(qualificationMatrix1.getId());
        assertThat(qualificationMatrix1).isEqualTo(qualificationMatrix2);

        qualificationMatrix2 = getQualificationMatrixSample2();
        assertThat(qualificationMatrix1).isNotEqualTo(qualificationMatrix2);
    }
}
