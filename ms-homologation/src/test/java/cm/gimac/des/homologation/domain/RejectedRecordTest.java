package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.RejectedRecordTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RejectedRecordTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(RejectedRecord.class);
        RejectedRecord rejectedRecord1 = getRejectedRecordSample1();
        RejectedRecord rejectedRecord2 = new RejectedRecord();
        assertThat(rejectedRecord1).isNotEqualTo(rejectedRecord2);

        rejectedRecord2.setId(rejectedRecord1.getId());
        assertThat(rejectedRecord1).isEqualTo(rejectedRecord2);

        rejectedRecord2 = getRejectedRecordSample2();
        assertThat(rejectedRecord1).isNotEqualTo(rejectedRecord2);
    }
}
