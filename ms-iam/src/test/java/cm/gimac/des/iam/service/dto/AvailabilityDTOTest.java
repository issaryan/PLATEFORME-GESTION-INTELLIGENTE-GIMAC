package cm.gimac.des.iam.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvailabilityDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AvailabilityDTO.class);
        AvailabilityDTO availabilityDTO1 = new AvailabilityDTO();
        availabilityDTO1.setId(UUID.randomUUID());
        AvailabilityDTO availabilityDTO2 = new AvailabilityDTO();
        assertThat(availabilityDTO1).isNotEqualTo(availabilityDTO2);
        availabilityDTO2.setId(availabilityDTO1.getId());
        assertThat(availabilityDTO1).isEqualTo(availabilityDTO2);
        availabilityDTO2.setId(UUID.randomUUID());
        assertThat(availabilityDTO1).isNotEqualTo(availabilityDTO2);
        availabilityDTO1.setId(null);
        assertThat(availabilityDTO1).isNotEqualTo(availabilityDTO2);
    }
}
