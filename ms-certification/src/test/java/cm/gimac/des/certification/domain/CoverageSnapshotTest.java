package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.CoverageSnapshotTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CoverageSnapshotTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CoverageSnapshot.class);
        CoverageSnapshot coverageSnapshot1 = getCoverageSnapshotSample1();
        CoverageSnapshot coverageSnapshot2 = new CoverageSnapshot();
        assertThat(coverageSnapshot1).isNotEqualTo(coverageSnapshot2);

        coverageSnapshot2.setId(coverageSnapshot1.getId());
        assertThat(coverageSnapshot1).isEqualTo(coverageSnapshot2);

        coverageSnapshot2 = getCoverageSnapshotSample2();
        assertThat(coverageSnapshot1).isNotEqualTo(coverageSnapshot2);
    }
}
