package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.ConciliationEntryTestSamples.*;
import static cm.gimac.des.rapprochement.domain.SuspensTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ConciliationEntryTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ConciliationEntry.class);
        ConciliationEntry conciliationEntry1 = getConciliationEntrySample1();
        ConciliationEntry conciliationEntry2 = new ConciliationEntry();
        assertThat(conciliationEntry1).isNotEqualTo(conciliationEntry2);

        conciliationEntry2.setId(conciliationEntry1.getId());
        assertThat(conciliationEntry1).isEqualTo(conciliationEntry2);

        conciliationEntry2 = getConciliationEntrySample2();
        assertThat(conciliationEntry1).isNotEqualTo(conciliationEntry2);
    }

    @Test
    void suspensTest() {
        ConciliationEntry conciliationEntry = getConciliationEntryRandomSampleGenerator();
        Suspens suspensBack = getSuspensRandomSampleGenerator();

        conciliationEntry.setSuspens(suspensBack);
        assertThat(conciliationEntry.getSuspens()).isEqualTo(suspensBack);

        conciliationEntry.suspens(null);
        assertThat(conciliationEntry.getSuspens()).isNull();
    }
}
