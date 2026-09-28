package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AbsenceProjectionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AbsenceProjectionDTO.class);
        AbsenceProjectionDTO absenceProjectionDTO1 = new AbsenceProjectionDTO();
        absenceProjectionDTO1.setId(UUID.randomUUID());
        AbsenceProjectionDTO absenceProjectionDTO2 = new AbsenceProjectionDTO();
        assertThat(absenceProjectionDTO1).isNotEqualTo(absenceProjectionDTO2);
        absenceProjectionDTO2.setId(absenceProjectionDTO1.getId());
        assertThat(absenceProjectionDTO1).isEqualTo(absenceProjectionDTO2);
        absenceProjectionDTO2.setId(UUID.randomUUID());
        assertThat(absenceProjectionDTO1).isNotEqualTo(absenceProjectionDTO2);
        absenceProjectionDTO1.setId(null);
        assertThat(absenceProjectionDTO1).isNotEqualTo(absenceProjectionDTO2);
    }
}
