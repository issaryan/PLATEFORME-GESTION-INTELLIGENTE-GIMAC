package cm.gimac.des.audit.domain;

import static cm.gimac.des.audit.domain.DeduplicationEntryTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.audit.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class DeduplicationEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(DeduplicationEntry.class);
        DeduplicationEntry deduplicationEntry1 = getDeduplicationEntrySample1();
        DeduplicationEntry deduplicationEntry2 = new DeduplicationEntry();
        assertThat(deduplicationEntry1).isNotEqualTo(deduplicationEntry2);

        deduplicationEntry2.setId(deduplicationEntry1.getId());
        assertThat(deduplicationEntry1).isEqualTo(deduplicationEntry2);

        deduplicationEntry2 = getDeduplicationEntrySample2();
        assertThat(deduplicationEntry1).isNotEqualTo(deduplicationEntry2);
    }
}
