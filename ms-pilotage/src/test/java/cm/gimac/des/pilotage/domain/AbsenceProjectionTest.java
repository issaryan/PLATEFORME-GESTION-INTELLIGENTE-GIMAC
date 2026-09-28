package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.AbsenceProjectionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AbsenceProjectionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AbsenceProjection.class);
        AbsenceProjection absenceProjection1 = getAbsenceProjectionSample1();
        AbsenceProjection absenceProjection2 = new AbsenceProjection();
        assertThat(absenceProjection1).isNotEqualTo(absenceProjection2);

        absenceProjection2.setId(absenceProjection1.getId());
        assertThat(absenceProjection1).isEqualTo(absenceProjection2);

        absenceProjection2 = getAbsenceProjectionSample2();
        assertThat(absenceProjection1).isNotEqualTo(absenceProjection2);
    }
}
