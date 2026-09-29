package cm.gimac.des.homologation.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferentialMigrationDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferentialMigrationDTO.class);
        ReferentialMigrationDTO referentialMigrationDTO1 = new ReferentialMigrationDTO();
        referentialMigrationDTO1.setId(UUID.randomUUID());
        ReferentialMigrationDTO referentialMigrationDTO2 = new ReferentialMigrationDTO();
        assertThat(referentialMigrationDTO1).isNotEqualTo(referentialMigrationDTO2);
        referentialMigrationDTO2.setId(referentialMigrationDTO1.getId());
        assertThat(referentialMigrationDTO1).isEqualTo(referentialMigrationDTO2);
        referentialMigrationDTO2.setId(UUID.randomUUID());
        assertThat(referentialMigrationDTO1).isNotEqualTo(referentialMigrationDTO2);
        referentialMigrationDTO1.setId(null);
        assertThat(referentialMigrationDTO1).isNotEqualTo(referentialMigrationDTO2);
    }
}
