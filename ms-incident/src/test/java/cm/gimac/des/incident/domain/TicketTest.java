package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.IncidentTestSamples.*;
import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Ticket.class);
        Ticket ticket1 = getTicketSample1();
        Ticket ticket2 = new Ticket();
        assertThat(ticket1).isNotEqualTo(ticket2);

        ticket2.setId(ticket1.getId());
        assertThat(ticket1).isEqualTo(ticket2);

        ticket2 = getTicketSample2();
        assertThat(ticket1).isNotEqualTo(ticket2);
    }

    @Test
    void relatedTicketTest() {
        Ticket ticket = getTicketRandomSampleGenerator();
        Ticket ticketBack = getTicketRandomSampleGenerator();

        ticket.setRelatedTicket(ticketBack);
        assertThat(ticket.getRelatedTicket()).isEqualTo(ticketBack);

        ticket.relatedTicket(null);
        assertThat(ticket.getRelatedTicket()).isNull();
    }

    @Test
    void incidentTest() {
        Ticket ticket = getTicketRandomSampleGenerator();
        Incident incidentBack = getIncidentRandomSampleGenerator();

        ticket.setIncident(incidentBack);
        assertThat(ticket.getIncident()).isEqualTo(incidentBack);
        assertThat(incidentBack.getTicket()).isEqualTo(ticket);

        ticket.incident(null);
        assertThat(ticket.getIncident()).isNull();
        assertThat(incidentBack.getTicket()).isNull();
    }
}
