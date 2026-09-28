package cm.gimac.des.document.domain;

import static cm.gimac.des.document.domain.IntegrityCheckTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.document.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IntegrityCheckTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(IntegrityCheck.class);
        IntegrityCheck integrityCheck1 = getIntegrityCheckSample1();
        IntegrityCheck integrityCheck2 = new IntegrityCheck();
        assertThat(integrityCheck1).isNotEqualTo(integrityCheck2);

        integrityCheck2.setId(integrityCheck1.getId());
        assertThat(integrityCheck1).isEqualTo(integrityCheck2);

        integrityCheck2 = getIntegrityCheckSample2();
        assertThat(integrityCheck1).isNotEqualTo(integrityCheck2);
    }
}
