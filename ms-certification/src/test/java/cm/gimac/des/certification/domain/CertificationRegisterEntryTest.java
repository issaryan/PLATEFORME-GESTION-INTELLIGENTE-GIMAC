package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CertificationRegisterEntryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CertificationRegisterEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CertificationRegisterEntry.class);
        CertificationRegisterEntry certificationRegisterEntry1 = getCertificationRegisterEntrySample1();
        CertificationRegisterEntry certificationRegisterEntry2 = new CertificationRegisterEntry();
        assertThat(certificationRegisterEntry1).isNotEqualTo(certificationRegisterEntry2);

        certificationRegisterEntry2.setId(certificationRegisterEntry1.getId());
        assertThat(certificationRegisterEntry1).isEqualTo(certificationRegisterEntry2);

        certificationRegisterEntry2 = getCertificationRegisterEntrySample2();
        assertThat(certificationRegisterEntry1).isNotEqualTo(certificationRegisterEntry2);
    }
}
