package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.VerifiedTerminalTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class VerifiedTerminalTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(VerifiedTerminal.class);
        VerifiedTerminal verifiedTerminal1 = getVerifiedTerminalSample1();
        VerifiedTerminal verifiedTerminal2 = new VerifiedTerminal();
        assertThat(verifiedTerminal1).isNotEqualTo(verifiedTerminal2);

        verifiedTerminal2.setId(verifiedTerminal1.getId());
        assertThat(verifiedTerminal1).isEqualTo(verifiedTerminal2);

        verifiedTerminal2 = getVerifiedTerminalSample2();
        assertThat(verifiedTerminal1).isNotEqualTo(verifiedTerminal2);
    }
}
