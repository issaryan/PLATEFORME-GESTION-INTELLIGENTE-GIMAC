package cm.gimac.des.homologation.domain;

import static cm.gimac.des.homologation.domain.ReplayCursorTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.homologation.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReplayCursorTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ReplayCursor.class);
        ReplayCursor replayCursor1 = getReplayCursorSample1();
        ReplayCursor replayCursor2 = new ReplayCursor();
        assertThat(replayCursor1).isNotEqualTo(replayCursor2);

        replayCursor2.setId(replayCursor1.getId());
        assertThat(replayCursor1).isEqualTo(replayCursor2);

        replayCursor2 = getReplayCursorSample2();
        assertThat(replayCursor1).isNotEqualTo(replayCursor2);
    }
}
