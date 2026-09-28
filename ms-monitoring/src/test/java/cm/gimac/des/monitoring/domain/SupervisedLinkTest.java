package cm.gimac.des.monitoring.domain;

import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;
import static cm.gimac.des.monitoring.domain.SupervisedLinkTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SupervisedLinkTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SupervisedLink.class);
        SupervisedLink supervisedLink1 = getSupervisedLinkSample1();
        SupervisedLink supervisedLink2 = new SupervisedLink();
        assertThat(supervisedLink1).isNotEqualTo(supervisedLink2);

        supervisedLink2.setId(supervisedLink1.getId());
        assertThat(supervisedLink1).isEqualTo(supervisedLink2);

        supervisedLink2 = getSupervisedLinkSample2();
        assertThat(supervisedLink1).isNotEqualTo(supervisedLink2);
    }

    @Test
    void parentPlatformTest() {
        SupervisedLink supervisedLink = getSupervisedLinkRandomSampleGenerator();
        SupervisedLink supervisedLinkBack = getSupervisedLinkRandomSampleGenerator();

        supervisedLink.setParentPlatform(supervisedLinkBack);
        assertThat(supervisedLink.getParentPlatform()).isEqualTo(supervisedLinkBack);

        supervisedLink.parentPlatform(null);
        assertThat(supervisedLink.getParentPlatform()).isNull();
    }
}
