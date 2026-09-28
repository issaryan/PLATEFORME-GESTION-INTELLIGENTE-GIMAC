package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.CoverageTestSamples.*;
import static cm.gimac.des.homologation.domain.LiaisonTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CoverageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Coverage.class);
        Coverage coverage1 = getCoverageSample1();
        Coverage coverage2 = new Coverage();
        assertThat(coverage1).isNotEqualTo(coverage2);

        coverage2.setId(coverage1.getId());
        assertThat(coverage1).isEqualTo(coverage2);

        coverage2 = getCoverageSample2();
        assertThat(coverage1).isNotEqualTo(coverage2);
    }

    @Test
    void liaisonTest() {
        Coverage coverage = getCoverageRandomSampleGenerator();
        Liaison liaisonBack = getLiaisonRandomSampleGenerator();

        coverage.setLiaison(liaisonBack);
        assertThat(coverage.getLiaison()).isEqualTo(liaisonBack);

        coverage.liaison(null);
        assertThat(coverage.getLiaison()).isNull();
    }
}
