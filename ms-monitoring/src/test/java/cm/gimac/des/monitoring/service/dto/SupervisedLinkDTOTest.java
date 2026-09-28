package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupervisedLinkDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SupervisedLinkDTO.class);
        SupervisedLinkDTO supervisedLinkDTO1 = new SupervisedLinkDTO();
        supervisedLinkDTO1.setId(UUID.randomUUID());
        SupervisedLinkDTO supervisedLinkDTO2 = new SupervisedLinkDTO();
        assertThat(supervisedLinkDTO1).isNotEqualTo(supervisedLinkDTO2);
        supervisedLinkDTO2.setId(supervisedLinkDTO1.getId());
        assertThat(supervisedLinkDTO1).isEqualTo(supervisedLinkDTO2);
        supervisedLinkDTO2.setId(UUID.randomUUID());
        assertThat(supervisedLinkDTO1).isNotEqualTo(supervisedLinkDTO2);
        supervisedLinkDTO1.setId(null);
        assertThat(supervisedLinkDTO1).isNotEqualTo(supervisedLinkDTO2);
    }
}
