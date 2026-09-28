package cm.gimac.des.incident.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrisisCellDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CrisisCellDTO.class);
        CrisisCellDTO crisisCellDTO1 = new CrisisCellDTO();
        crisisCellDTO1.setId(UUID.randomUUID());
        CrisisCellDTO crisisCellDTO2 = new CrisisCellDTO();
        assertThat(crisisCellDTO1).isNotEqualTo(crisisCellDTO2);
        crisisCellDTO2.setId(crisisCellDTO1.getId());
        assertThat(crisisCellDTO1).isEqualTo(crisisCellDTO2);
        crisisCellDTO2.setId(UUID.randomUUID());
        assertThat(crisisCellDTO1).isNotEqualTo(crisisCellDTO2);
        crisisCellDTO1.setId(null);
        assertThat(crisisCellDTO1).isNotEqualTo(crisisCellDTO2);
    }
}
