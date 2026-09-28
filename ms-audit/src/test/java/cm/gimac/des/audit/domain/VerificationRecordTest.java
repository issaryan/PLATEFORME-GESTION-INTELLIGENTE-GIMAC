package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.InclusionProofTestSamples.*;
import static cm.gimac.des.audit.domain.VerificationRecordTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class VerificationRecordTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(VerificationRecord.class);
        VerificationRecord verificationRecord1 = getVerificationRecordSample1();
        VerificationRecord verificationRecord2 = new VerificationRecord();
        assertThat(verificationRecord1).isNotEqualTo(verificationRecord2);

        verificationRecord2.setId(verificationRecord1.getId());
        assertThat(verificationRecord1).isEqualTo(verificationRecord2);

        verificationRecord2 = getVerificationRecordSample2();
        assertThat(verificationRecord1).isNotEqualTo(verificationRecord2);
    }

    @Test
    void proofTest() {
        VerificationRecord verificationRecord = getVerificationRecordRandomSampleGenerator();
        InclusionProof inclusionProofBack = getInclusionProofRandomSampleGenerator();

        verificationRecord.setProof(inclusionProofBack);
        assertThat(verificationRecord.getProof()).isEqualTo(inclusionProofBack);

        verificationRecord.proof(null);
        assertThat(verificationRecord.getProof()).isNull();
    }
}
