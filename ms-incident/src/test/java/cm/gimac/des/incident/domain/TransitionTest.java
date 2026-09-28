package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static cm.gimac.des.incident.domain.TransitionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TransitionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Transition.class);
        Transition transition1 = getTransitionSample1();
        Transition transition2 = new Transition();
        assertThat(transition1).isNotEqualTo(transition2);

        transition2.setId(transition1.getId());
        assertThat(transition1).isEqualTo(transition2);

        transition2 = getTransitionSample2();
        assertThat(transition1).isNotEqualTo(transition2);
    }

    @Test
    void ticketTest() {
        Transition transition = getTransitionRandomSampleGenerator();
        Ticket ticketBack = getTicketRandomSampleGenerator();

        transition.setTicket(ticketBack);
        assertThat(transition.getTicket()).isEqualTo(ticketBack);

        transition.ticket(null);
        assertThat(transition.getTicket()).isNull();
    }
}
