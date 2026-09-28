package cm.gimac.des.incident.domain;

import static cm.gimac.des.incident.domain.ServiceCommitmentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.incident.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ServiceCommitmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ServiceCommitment.class);
        ServiceCommitment serviceCommitment1 = getServiceCommitmentSample1();
        ServiceCommitment serviceCommitment2 = new ServiceCommitment();
        assertThat(serviceCommitment1).isNotEqualTo(serviceCommitment2);

        serviceCommitment2.setId(serviceCommitment1.getId());
        assertThat(serviceCommitment1).isEqualTo(serviceCommitment2);

        serviceCommitment2 = getServiceCommitmentSample2();
        assertThat(serviceCommitment1).isNotEqualTo(serviceCommitment2);
    }
}
