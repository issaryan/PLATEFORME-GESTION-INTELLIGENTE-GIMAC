package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.CorrespondentTestSamples.*;
import static cm.gimac.des.participant.domain.EscalationContactTestSamples.*;
import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EscalationContactTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EscalationContact.class);
        EscalationContact escalationContact1 = getEscalationContactSample1();
        EscalationContact escalationContact2 = new EscalationContact();
        assertThat(escalationContact1).isNotEqualTo(escalationContact2);

        escalationContact2.setId(escalationContact1.getId());
        assertThat(escalationContact1).isEqualTo(escalationContact2);

        escalationContact2 = getEscalationContactSample2();
        assertThat(escalationContact1).isNotEqualTo(escalationContact2);
    }

    @Test
    void institutionTest() {
        EscalationContact escalationContact = getEscalationContactRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        escalationContact.setInstitution(institutionBack);
        assertThat(escalationContact.getInstitution()).isEqualTo(institutionBack);

        escalationContact.institution(null);
        assertThat(escalationContact.getInstitution()).isNull();
    }

    @Test
    void correspondentTest() {
        EscalationContact escalationContact = getEscalationContactRandomSampleGenerator();
        Correspondent correspondentBack = getCorrespondentRandomSampleGenerator();

        escalationContact.setCorrespondent(correspondentBack);
        assertThat(escalationContact.getCorrespondent()).isEqualTo(correspondentBack);

        escalationContact.correspondent(null);
        assertThat(escalationContact.getCorrespondent()).isNull();
    }

    @Test
    void deputyTest() {
        EscalationContact escalationContact = getEscalationContactRandomSampleGenerator();
        Correspondent correspondentBack = getCorrespondentRandomSampleGenerator();

        escalationContact.setDeputy(correspondentBack);
        assertThat(escalationContact.getDeputy()).isEqualTo(correspondentBack);

        escalationContact.deputy(null);
        assertThat(escalationContact.getDeputy()).isNull();
    }
}
