package cm.gimac.des.participant.domain;

import static cm.gimac.des.participant.domain.InstitutionTestSamples.*;
import static cm.gimac.des.participant.domain.SubscribedServiceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SubscribedServiceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubscribedService.class);
        SubscribedService subscribedService1 = getSubscribedServiceSample1();
        SubscribedService subscribedService2 = new SubscribedService();
        assertThat(subscribedService1).isNotEqualTo(subscribedService2);

        subscribedService2.setId(subscribedService1.getId());
        assertThat(subscribedService1).isEqualTo(subscribedService2);

        subscribedService2 = getSubscribedServiceSample2();
        assertThat(subscribedService1).isNotEqualTo(subscribedService2);
    }

    @Test
    void institutionTest() {
        SubscribedService subscribedService = getSubscribedServiceRandomSampleGenerator();
        Institution institutionBack = getInstitutionRandomSampleGenerator();

        subscribedService.setInstitution(institutionBack);
        assertThat(subscribedService.getInstitution()).isEqualTo(institutionBack);

        subscribedService.institution(null);
        assertThat(subscribedService.getInstitution()).isNull();
    }
}
