package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.DimensionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DimensionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Dimension.class);
        Dimension dimension1 = getDimensionSample1();
        Dimension dimension2 = new Dimension();
        assertThat(dimension1).isNotEqualTo(dimension2);

        dimension2.setId(dimension1.getId());
        assertThat(dimension1).isEqualTo(dimension2);

        dimension2 = getDimensionSample2();
        assertThat(dimension1).isNotEqualTo(dimension2);
    }
}
