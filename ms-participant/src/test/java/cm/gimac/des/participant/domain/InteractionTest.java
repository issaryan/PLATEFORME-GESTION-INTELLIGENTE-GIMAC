package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.InteractionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class InteractionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Interaction.class);
        Interaction interaction1 = getInteractionSample1();
        Interaction interaction2 = new Interaction();
        assertThat(interaction1).isNotEqualTo(interaction2);

        interaction2.setId(interaction1.getId());
        assertThat(interaction1).isEqualTo(interaction2);

        interaction2 = getInteractionSample2();
        assertThat(interaction1).isNotEqualTo(interaction2);
    }

    @Test
    void institutionTest() {
        Interaction interaction = getInteractionRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        interaction.setInstitution(institutionBack);
        assertThat(interaction.getInstitution()).isEqualTo(institutionBack);

        interaction.institution(null);
        assertThat(interaction.getInstitution()).isNull();
    }
}
