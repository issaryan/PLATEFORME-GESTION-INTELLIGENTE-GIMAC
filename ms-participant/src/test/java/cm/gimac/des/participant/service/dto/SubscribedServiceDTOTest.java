package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscribedServiceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscribedServiceDTO.class);
        SubscribedServiceDTO subscribedServiceDTO1 = new SubscribedServiceDTO();
        subscribedServiceDTO1.setId(UUID.randomUUID());
        SubscribedServiceDTO subscribedServiceDTO2 = new SubscribedServiceDTO();
        assertThat(subscribedServiceDTO1).isNotEqualTo(subscribedServiceDTO2);
        subscribedServiceDTO2.setId(subscribedServiceDTO1.getId());
        assertThat(subscribedServiceDTO1).isEqualTo(subscribedServiceDTO2);
        subscribedServiceDTO2.setId(UUID.randomUUID());
        assertThat(subscribedServiceDTO1).isNotEqualTo(subscribedServiceDTO2);
        subscribedServiceDTO1.setId(null);
        assertThat(subscribedServiceDTO1).isNotEqualTo(subscribedServiceDTO2);
    }
}
