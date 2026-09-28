package cm.gimac.des.pilotage.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionSheetDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ActionSheetDTO.class);
        ActionSheetDTO actionSheetDTO1 = new ActionSheetDTO();
        actionSheetDTO1.setId(UUID.randomUUID());
        ActionSheetDTO actionSheetDTO2 = new ActionSheetDTO();
        assertThat(actionSheetDTO1).isNotEqualTo(actionSheetDTO2);
        actionSheetDTO2.setId(actionSheetDTO1.getId());
        assertThat(actionSheetDTO1).isEqualTo(actionSheetDTO2);
        actionSheetDTO2.setId(UUID.randomUUID());
        assertThat(actionSheetDTO1).isNotEqualTo(actionSheetDTO2);
        actionSheetDTO1.setId(null);
        assertThat(actionSheetDTO1).isNotEqualTo(actionSheetDTO2);
    }
}
