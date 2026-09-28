package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.ClosedOperationSnapshotTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ClosedOperationSnapshotTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ClosedOperationSnapshot.class);
        ClosedOperationSnapshot closedOperationSnapshot1 = getClosedOperationSnapshotSample1();
        ClosedOperationSnapshot closedOperationSnapshot2 = new ClosedOperationSnapshot();
        assertThat(closedOperationSnapshot1).isNotEqualTo(closedOperationSnapshot2);

        closedOperationSnapshot2.setId(closedOperationSnapshot1.getId());
        assertThat(closedOperationSnapshot1).isEqualTo(closedOperationSnapshot2);

        closedOperationSnapshot2 = getClosedOperationSnapshotSample2();
        assertThat(closedOperationSnapshot1).isNotEqualTo(closedOperationSnapshot2);
    }
}
