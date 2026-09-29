package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReferentialVersionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReferentialVersionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferentialVersion.class);
        ReferentialVersion referentialVersion1 = getReferentialVersionSample1();
        ReferentialVersion referentialVersion2 = new ReferentialVersion();
        assertThat(referentialVersion1).isNotEqualTo(referentialVersion2);

        referentialVersion2.setId(referentialVersion1.getId());
        assertThat(referentialVersion1).isEqualTo(referentialVersion2);

        referentialVersion2 = getReferentialVersionSample2();
        assertThat(referentialVersion1).isNotEqualTo(referentialVersion2);
    }
}
