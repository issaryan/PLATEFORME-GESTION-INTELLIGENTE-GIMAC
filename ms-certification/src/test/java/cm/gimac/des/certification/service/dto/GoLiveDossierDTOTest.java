package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GoLiveDossierDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(GoLiveDossierDTO.class);
        GoLiveDossierDTO goLiveDossierDTO1 = new GoLiveDossierDTO();
        goLiveDossierDTO1.setId(UUID.randomUUID());
        GoLiveDossierDTO goLiveDossierDTO2 = new GoLiveDossierDTO();
        assertThat(goLiveDossierDTO1).isNotEqualTo(goLiveDossierDTO2);
        goLiveDossierDTO2.setId(goLiveDossierDTO1.getId());
        assertThat(goLiveDossierDTO1).isEqualTo(goLiveDossierDTO2);
        goLiveDossierDTO2.setId(UUID.randomUUID());
        assertThat(goLiveDossierDTO1).isNotEqualTo(goLiveDossierDTO2);
        goLiveDossierDTO1.setId(null);
        assertThat(goLiveDossierDTO1).isNotEqualTo(goLiveDossierDTO2);
    }
}
