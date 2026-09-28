package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuitusDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(QuitusDTO.class);
        QuitusDTO quitusDTO1 = new QuitusDTO();
        quitusDTO1.setId(UUID.randomUUID());
        QuitusDTO quitusDTO2 = new QuitusDTO();
        assertThat(quitusDTO1).isNotEqualTo(quitusDTO2);
        quitusDTO2.setId(quitusDTO1.getId());
        assertThat(quitusDTO1).isEqualTo(quitusDTO2);
        quitusDTO2.setId(UUID.randomUUID());
        assertThat(quitusDTO1).isNotEqualTo(quitusDTO2);
        quitusDTO1.setId(null);
        assertThat(quitusDTO1).isNotEqualTo(quitusDTO2);
    }
}
