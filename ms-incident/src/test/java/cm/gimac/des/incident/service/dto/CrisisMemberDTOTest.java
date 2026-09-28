package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrisisMemberDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisMemberDTO.class);
        CrisisMemberDTO crisisMemberDTO1 = new CrisisMemberDTO();
        crisisMemberDTO1.setId(UUID.randomUUID());
        CrisisMemberDTO crisisMemberDTO2 = new CrisisMemberDTO();
        assertThat(crisisMemberDTO1).isNotEqualTo(crisisMemberDTO2);
        crisisMemberDTO2.setId(crisisMemberDTO1.getId());
        assertThat(crisisMemberDTO1).isEqualTo(crisisMemberDTO2);
        crisisMemberDTO2.setId(UUID.randomUUID());
        assertThat(crisisMemberDTO1).isNotEqualTo(crisisMemberDTO2);
        crisisMemberDTO1.setId(null);
        assertThat(crisisMemberDTO1).isNotEqualTo(crisisMemberDTO2);
    }
}
