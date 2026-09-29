package cm.gimac.des.certification.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SignedDecisionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SignedDecisionDTO.class);
        SignedDecisionDTO signedDecisionDTO1 = new SignedDecisionDTO();
        signedDecisionDTO1.setId(UUID.randomUUID());
        SignedDecisionDTO signedDecisionDTO2 = new SignedDecisionDTO();
        assertThat(signedDecisionDTO1).isNotEqualTo(signedDecisionDTO2);
        signedDecisionDTO2.setId(signedDecisionDTO1.getId());
        assertThat(signedDecisionDTO1).isEqualTo(signedDecisionDTO2);
        signedDecisionDTO2.setId(UUID.randomUUID());
        assertThat(signedDecisionDTO1).isNotEqualTo(signedDecisionDTO2);
        signedDecisionDTO1.setId(null);
        assertThat(signedDecisionDTO1).isNotEqualTo(signedDecisionDTO2);
    }
}
