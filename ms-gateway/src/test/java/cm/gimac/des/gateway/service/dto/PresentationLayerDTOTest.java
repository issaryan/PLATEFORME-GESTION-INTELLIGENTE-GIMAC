package cm.gimac.des.gateway.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.gateway.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PresentationLayerDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PresentationLayerDTO.class);
        PresentationLayerDTO presentationLayerDTO1 = new PresentationLayerDTO();
        presentationLayerDTO1.setId(1L);
        PresentationLayerDTO presentationLayerDTO2 = new PresentationLayerDTO();
        assertThat(presentationLayerDTO1).isNotEqualTo(presentationLayerDTO2);
        presentationLayerDTO2.setId(presentationLayerDTO1.getId());
        assertThat(presentationLayerDTO1).isEqualTo(presentationLayerDTO2);
        presentationLayerDTO2.setId(2L);
        assertThat(presentationLayerDTO1).isNotEqualTo(presentationLayerDTO2);
        presentationLayerDTO1.setId(null);
        assertThat(presentationLayerDTO1).isNotEqualTo(presentationLayerDTO2);
    }
}
