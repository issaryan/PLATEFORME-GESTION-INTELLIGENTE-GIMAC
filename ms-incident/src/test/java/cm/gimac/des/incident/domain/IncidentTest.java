package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.IncidentTestSamples.*;
import static cm.gimac.des.incident.domain.QualificationMatrixTestSamples.*;
import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class IncidentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Incident.class);
        Incident incident1 = getIncidentSample1();
        Incident incident2 = new Incident();
        assertThat(incident1).isNotEqualTo(incident2);

        incident2.setId(incident1.getId());
        assertThat(incident1).isEqualTo(incident2);

        incident2 = getIncidentSample2();
        assertThat(incident1).isNotEqualTo(incident2);
    }

    @Test
    void ticketTest() {
        Incident incident = getIncidentRandomSampleGenerator();
        Ticket ticketBack = getTicketRandomSampleGenerator();

        incident.setTicket(ticketBack);
        assertThat(incident.getTicket()).isEqualTo(ticketBack);

        incident.ticket(null);
        assertThat(incident.getTicket()).isNull();
    }

    @Test
    void matrixTest() {
        Incident incident = getIncidentRandomSampleGenerator();
        QualificationMatrix qualificationMatrixBack = getQualificationMatrixRandomSampleGenerator();

        incident.setMatrix(qualificationMatrixBack);
        assertThat(incident.getMatrix()).isEqualTo(qualificationMatrixBack);

        incident.matrix(null);
        assertThat(incident.getMatrix()).isNull();
    }
}
