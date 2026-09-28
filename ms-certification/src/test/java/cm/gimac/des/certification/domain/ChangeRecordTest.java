package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.ChangeRecordTestSamples.*;
import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ChangeRecordTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChangeRecord.class);
        ChangeRecord changeRecord1 = getChangeRecordSample1();
        ChangeRecord changeRecord2 = new ChangeRecord();
        assertThat(changeRecord1).isNotEqualTo(changeRecord2);

        changeRecord2.setId(changeRecord1.getId());
        assertThat(changeRecord1).isEqualTo(changeRecord2);

        changeRecord2 = getChangeRecordSample2();
        assertThat(changeRecord1).isNotEqualTo(changeRecord2);
    }

    @Test
    void commissioningFileTest() {
        ChangeRecord changeRecord = getChangeRecordRandomSampleGenerator();
        CommissioningFile commissioningFileBack = getCommissioningFileRandomSampleGenerator();

        changeRecord.setCommissioningFile(commissioningFileBack);
        assertThat(changeRecord.getCommissioningFile()).isEqualTo(commissioningFileBack);
        assertThat(commissioningFileBack.getChangeRecord()).isEqualTo(changeRecord);

        changeRecord.commissioningFile(null);
        assertThat(changeRecord.getCommissioningFile()).isNull();
        assertThat(commissioningFileBack.getChangeRecord()).isNull();
    }
}
