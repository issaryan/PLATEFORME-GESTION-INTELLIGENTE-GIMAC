package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConventionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConventionDTO.class);
        ConventionDTO conventionDTO1 = new ConventionDTO();
        conventionDTO1.setId(UUID.randomUUID());
        ConventionDTO conventionDTO2 = new ConventionDTO();
        assertThat(conventionDTO1).isNotEqualTo(conventionDTO2);
        conventionDTO2.setId(conventionDTO1.getId());
        assertThat(conventionDTO1).isEqualTo(conventionDTO2);
        conventionDTO2.setId(UUID.randomUUID());
        assertThat(conventionDTO1).isNotEqualTo(conventionDTO2);
        conventionDTO1.setId(null);
        assertThat(conventionDTO1).isNotEqualTo(conventionDTO2);
    }
}
