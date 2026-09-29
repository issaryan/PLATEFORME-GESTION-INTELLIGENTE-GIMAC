package cm.gimac.des.participant.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdhesionPieceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdhesionPieceDTO.class);
        AdhesionPieceDTO adhesionPieceDTO1 = new AdhesionPieceDTO();
        adhesionPieceDTO1.setId(UUID.randomUUID());
        AdhesionPieceDTO adhesionPieceDTO2 = new AdhesionPieceDTO();
        assertThat(adhesionPieceDTO1).isNotEqualTo(adhesionPieceDTO2);
        adhesionPieceDTO2.setId(adhesionPieceDTO1.getId());
        assertThat(adhesionPieceDTO1).isEqualTo(adhesionPieceDTO2);
        adhesionPieceDTO2.setId(UUID.randomUUID());
        assertThat(adhesionPieceDTO1).isNotEqualTo(adhesionPieceDTO2);
        adhesionPieceDTO1.setId(null);
        assertThat(adhesionPieceDTO1).isNotEqualTo(adhesionPieceDTO2);
    }
}
