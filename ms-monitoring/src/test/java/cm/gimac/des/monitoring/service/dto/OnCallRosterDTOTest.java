package cm.gimac.des.monitoring.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.monitoring.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnCallRosterDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(OnCallRosterDTO.class);
        OnCallRosterDTO onCallRosterDTO1 = new OnCallRosterDTO();
        onCallRosterDTO1.setId(UUID.randomUUID());
        OnCallRosterDTO onCallRosterDTO2 = new OnCallRosterDTO();
        assertThat(onCallRosterDTO1).isNotEqualTo(onCallRosterDTO2);
        onCallRosterDTO2.setId(onCallRosterDTO1.getId());
        assertThat(onCallRosterDTO1).isEqualTo(onCallRosterDTO2);
        onCallRosterDTO2.setId(UUID.randomUUID());
        assertThat(onCallRosterDTO1).isNotEqualTo(onCallRosterDTO2);
        onCallRosterDTO1.setId(null);
        assertThat(onCallRosterDTO1).isNotEqualTo(onCallRosterDTO2);
    }
}
