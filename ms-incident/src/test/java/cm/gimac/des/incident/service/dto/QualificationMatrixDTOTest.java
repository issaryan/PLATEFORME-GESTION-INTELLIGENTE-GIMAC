package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QualificationMatrixDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(QualificationMatrixDTO.class);
        QualificationMatrixDTO qualificationMatrixDTO1 = new QualificationMatrixDTO();
        qualificationMatrixDTO1.setId(UUID.randomUUID());
        QualificationMatrixDTO qualificationMatrixDTO2 = new QualificationMatrixDTO();
        assertThat(qualificationMatrixDTO1).isNotEqualTo(qualificationMatrixDTO2);
        qualificationMatrixDTO2.setId(qualificationMatrixDTO1.getId());
        assertThat(qualificationMatrixDTO1).isEqualTo(qualificationMatrixDTO2);
        qualificationMatrixDTO2.setId(UUID.randomUUID());
        assertThat(qualificationMatrixDTO1).isNotEqualTo(qualificationMatrixDTO2);
        qualificationMatrixDTO1.setId(null);
        assertThat(qualificationMatrixDTO1).isNotEqualTo(qualificationMatrixDTO2);
    }
}
