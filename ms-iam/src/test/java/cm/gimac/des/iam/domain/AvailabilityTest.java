package cm.gimac.des.iam.domain;

import static cm.gimac.des.iam.domain.AvailabilityTestSamples.*;
import static cm.gimac.des.iam.domain.CollaboratorTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.iam.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AvailabilityTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Availability.class);
        Availability availability1 = getAvailabilitySample1();
        Availability availability2 = new Availability();
        assertThat(availability1).isNotEqualTo(availability2);

        availability2.setId(availability1.getId());
        assertThat(availability1).isEqualTo(availability2);

        availability2 = getAvailabilitySample2();
        assertThat(availability1).isNotEqualTo(availability2);
    }

    @Test
    void collaboratorTest() {
        Availability availability = getAvailabilityRandomSampleGenerator();
        Collaborator collaboratorBack = getCollaboratorRandomSampleGenerator();

        availability.setCollaborator(collaboratorBack);
        assertThat(availability.getCollaborator()).isEqualTo(collaboratorBack);

        availability.collaborator(null);
        assertThat(availability.getCollaborator()).isNull();
    }
}
