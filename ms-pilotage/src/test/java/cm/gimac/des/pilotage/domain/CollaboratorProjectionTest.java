package cm.gimac.des.pilotage.domain;

import static cm.gimac.des.pilotage.domain.CollaboratorProjectionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.pilotage.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CollaboratorProjectionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CollaboratorProjection.class);
        CollaboratorProjection collaboratorProjection1 = getCollaboratorProjectionSample1();
        CollaboratorProjection collaboratorProjection2 = new CollaboratorProjection();
        assertThat(collaboratorProjection1).isNotEqualTo(collaboratorProjection2);

        collaboratorProjection2.setId(collaboratorProjection1.getId());
        assertThat(collaboratorProjection1).isEqualTo(collaboratorProjection2);

        collaboratorProjection2 = getCollaboratorProjectionSample2();
        assertThat(collaboratorProjection1).isNotEqualTo(collaboratorProjection2);
    }
}
