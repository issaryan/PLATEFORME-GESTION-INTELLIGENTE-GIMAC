package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CommissioningFileTestSamples.*;
import static cm.gimac.des.certification.domain.EvidenceItemTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EvidenceItemTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EvidenceItem.class);
        EvidenceItem evidenceItem1 = getEvidenceItemSample1();
        EvidenceItem evidenceItem2 = new EvidenceItem();
        assertThat(evidenceItem1).isNotEqualTo(evidenceItem2);

        evidenceItem2.setId(evidenceItem1.getId());
        assertThat(evidenceItem1).isEqualTo(evidenceItem2);

        evidenceItem2 = getEvidenceItemSample2();
        assertThat(evidenceItem1).isNotEqualTo(evidenceItem2);
    }

    @Test
    void fileTest() {
        EvidenceItem evidenceItem = getEvidenceItemRandomSampleGenerator();
        CommissioningFile commissioningFileBack = getCommissioningFileRandomSampleGenerator();

        evidenceItem.setFile(commissioningFileBack);
        assertThat(evidenceItem.getFile()).isEqualTo(commissioningFileBack);

        evidenceItem.file(null);
        assertThat(evidenceItem.getFile()).isNull();
    }

    @Test
    void commissioningFileTest() {
        EvidenceItem evidenceItem = getEvidenceItemRandomSampleGenerator();
        CommissioningFile commissioningFileBack = getCommissioningFileRandomSampleGenerator();

        evidenceItem.setCommissioningFile(commissioningFileBack);
        assertThat(evidenceItem.getCommissioningFile()).isEqualTo(commissioningFileBack);
        assertThat(commissioningFileBack.getCommissioningReport()).isEqualTo(evidenceItem);

        evidenceItem.commissioningFile(null);
        assertThat(evidenceItem.getCommissioningFile()).isNull();
        assertThat(commissioningFileBack.getCommissioningReport()).isNull();
    }
}
