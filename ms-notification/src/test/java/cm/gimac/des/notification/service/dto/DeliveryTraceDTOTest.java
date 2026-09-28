package cm.gimac.des.notification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.notification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryTraceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeliveryTraceDTO.class);
        DeliveryTraceDTO deliveryTraceDTO1 = new DeliveryTraceDTO();
        deliveryTraceDTO1.setId(UUID.randomUUID());
        DeliveryTraceDTO deliveryTraceDTO2 = new DeliveryTraceDTO();
        assertThat(deliveryTraceDTO1).isNotEqualTo(deliveryTraceDTO2);
        deliveryTraceDTO2.setId(deliveryTraceDTO1.getId());
        assertThat(deliveryTraceDTO1).isEqualTo(deliveryTraceDTO2);
        deliveryTraceDTO2.setId(UUID.randomUUID());
        assertThat(deliveryTraceDTO1).isNotEqualTo(deliveryTraceDTO2);
        deliveryTraceDTO1.setId(null);
        assertThat(deliveryTraceDTO1).isNotEqualTo(deliveryTraceDTO2);
    }
}
