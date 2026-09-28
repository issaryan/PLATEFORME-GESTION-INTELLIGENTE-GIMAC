package cm.gimac.des.gateway.domain;

import static cm.gimac.des.gateway.domain.PresentationLayerTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PresentationLayerTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PresentationLayer.class);
        PresentationLayer presentationLayer1 = getPresentationLayerSample1();
        PresentationLayer presentationLayer2 = new PresentationLayer();
        assertThat(presentationLayer1).isNotEqualTo(presentationLayer2);

        presentationLayer2.setId(presentationLayer1.getId());
        assertThat(presentationLayer1).isEqualTo(presentationLayer2);

        presentationLayer2 = getPresentationLayerSample2();
        assertThat(presentationLayer1).isNotEqualTo(presentationLayer2);
    }
}
