package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReferentialMigrationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReferentialMigrationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReferentialMigration.class);
        ReferentialMigration referentialMigration1 = getReferentialMigrationSample1();
        ReferentialMigration referentialMigration2 = new ReferentialMigration();
        assertThat(referentialMigration1).isNotEqualTo(referentialMigration2);

        referentialMigration2.setId(referentialMigration1.getId());
        assertThat(referentialMigration1).isEqualTo(referentialMigration2);

        referentialMigration2 = getReferentialMigrationSample2();
        assertThat(referentialMigration1).isNotEqualTo(referentialMigration2);
    }
}
