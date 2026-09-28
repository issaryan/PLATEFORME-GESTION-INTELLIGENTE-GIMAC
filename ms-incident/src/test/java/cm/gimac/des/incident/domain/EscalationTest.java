package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.EscalationTestSamples.*;
import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EscalationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Escalation.class);
        Escalation escalation1 = getEscalationSample1();
        Escalation escalation2 = new Escalation();
        assertThat(escalation1).isNotEqualTo(escalation2);

        escalation2.setId(escalation1.getId());
        assertThat(escalation1).isEqualTo(escalation2);

        escalation2 = getEscalationSample2();
        assertThat(escalation1).isNotEqualTo(escalation2);
    }

    @Test
    void ticketTest() {
        Escalation escalation = getEscalationRandomSampleGenerator();
        Ticket ticketBack = getTicketRandomSampleGenerator();

        escalation.setTicket(ticketBack);
        assertThat(escalation.getTicket()).isEqualTo(ticketBack);

        escalation.ticket(null);
        assertThat(escalation.getTicket()).isNull();
    }
}
