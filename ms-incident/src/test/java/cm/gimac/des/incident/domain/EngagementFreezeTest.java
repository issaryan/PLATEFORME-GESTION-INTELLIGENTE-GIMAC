package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.EngagementFreezeTestSamples.*;
import static cm.gimac.des.incident.domain.TicketTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EngagementFreezeTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EngagementFreeze.class);
        EngagementFreeze engagementFreeze1 = getEngagementFreezeSample1();
        EngagementFreeze engagementFreeze2 = new EngagementFreeze();
        assertThat(engagementFreeze1).isNotEqualTo(engagementFreeze2);

        engagementFreeze2.setId(engagementFreeze1.getId());
        assertThat(engagementFreeze1).isEqualTo(engagementFreeze2);

        engagementFreeze2 = getEngagementFreezeSample2();
        assertThat(engagementFreeze1).isNotEqualTo(engagementFreeze2);
    }

    @Test
    void ticketTest() {
        EngagementFreeze engagementFreeze = getEngagementFreezeRandomSampleGenerator();
        Ticket ticketBack = getTicketRandomSampleGenerator();

        engagementFreeze.setTicket(ticketBack);
        assertThat(engagementFreeze.getTicket()).isEqualTo(ticketBack);

        engagementFreeze.ticket(null);
        assertThat(engagementFreeze.getTicket()).isNull();
    }
}
