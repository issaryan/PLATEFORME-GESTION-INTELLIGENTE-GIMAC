package cm.gimac.des.gateway.domain;

import static cm.gimac.des.gateway.domain.PresentationLayerTestSamples.*;
import static cm.gimac.des.gateway.domain.RouteTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class RouteTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Route.class);
        Route route1 = getRouteSample1();
        Route route2 = new Route();
        assertThat(route1).isNotEqualTo(route2);

        route2.setId(route1.getId());
        assertThat(route1).isEqualTo(route2);

        route2 = getRouteSample2();
        assertThat(route1).isNotEqualTo(route2);
    }

    @Test
    void layerTest() {
        Route route = getRouteRandomSampleGenerator();
        PresentationLayer presentationLayerBack = getPresentationLayerRandomSampleGenerator();

        route.setLayer(presentationLayerBack);
        assertThat(route.getLayer()).isEqualTo(presentationLayerBack);

        route.layer(null);
        assertThat(route.getLayer()).isNull();
    }
}
