package cm.gimac.des.rapprochement.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegularizationAttestationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(RegularizationAttestationDTO.class);
        RegularizationAttestationDTO regularizationAttestationDTO1 = new RegularizationAttestationDTO();
        regularizationAttestationDTO1.setId(UUID.randomUUID());
        RegularizationAttestationDTO regularizationAttestationDTO2 = new RegularizationAttestationDTO();
        assertThat(regularizationAttestationDTO1).isNotEqualTo(regularizationAttestationDTO2);
        regularizationAttestationDTO2.setId(regularizationAttestationDTO1.getId());
        assertThat(regularizationAttestationDTO1).isEqualTo(regularizationAttestationDTO2);
        regularizationAttestationDTO2.setId(UUID.randomUUID());
        assertThat(regularizationAttestationDTO1).isNotEqualTo(regularizationAttestationDTO2);
        regularizationAttestationDTO1.setId(null);
        assertThat(regularizationAttestationDTO1).isNotEqualTo(regularizationAttestationDTO2);
    }
}
