package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InteractionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(InteractionDTO.class);
        InteractionDTO interactionDTO1 = new InteractionDTO();
        interactionDTO1.setId(UUID.randomUUID());
        InteractionDTO interactionDTO2 = new InteractionDTO();
        assertThat(interactionDTO1).isNotEqualTo(interactionDTO2);
        interactionDTO2.setId(interactionDTO1.getId());
        assertThat(interactionDTO1).isEqualTo(interactionDTO2);
        interactionDTO2.setId(UUID.randomUUID());
        assertThat(interactionDTO1).isNotEqualTo(interactionDTO2);
        interactionDTO1.setId(null);
        assertThat(interactionDTO1).isNotEqualTo(interactionDTO2);
    }
}
