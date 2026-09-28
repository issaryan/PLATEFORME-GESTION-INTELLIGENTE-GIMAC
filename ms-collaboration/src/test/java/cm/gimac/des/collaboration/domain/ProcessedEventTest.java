package cm.gimac.des.collaboration.domain;

import static cm.gimac.des.collaboration.domain.ProcessedEventTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.collaboration.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProcessedEventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProcessedEvent.class);
        ProcessedEvent processedEvent1 = getProcessedEventSample1();
        ProcessedEvent processedEvent2 = new ProcessedEvent();
        assertThat(processedEvent1).isNotEqualTo(processedEvent2);

        processedEvent2.setId(processedEvent1.getId());
        assertThat(processedEvent1).isEqualTo(processedEvent2);

        processedEvent2 = getProcessedEventSample2();
        assertThat(processedEvent1).isNotEqualTo(processedEvent2);
    }
}
