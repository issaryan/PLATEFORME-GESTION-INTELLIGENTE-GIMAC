package cm.gimac.des.certification.domain;

import static cm.gimac.des.certification.domain.TimestampTokenTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.certification.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TimestampTokenTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TimestampToken.class);
        TimestampToken timestampToken1 = getTimestampTokenSample1();
        TimestampToken timestampToken2 = new TimestampToken();
        assertThat(timestampToken1).isNotEqualTo(timestampToken2);

        timestampToken2.setId(timestampToken1.getId());
        assertThat(timestampToken1).isEqualTo(timestampToken2);

        timestampToken2 = getTimestampTokenSample2();
        assertThat(timestampToken1).isNotEqualTo(timestampToken2);
    }
}
