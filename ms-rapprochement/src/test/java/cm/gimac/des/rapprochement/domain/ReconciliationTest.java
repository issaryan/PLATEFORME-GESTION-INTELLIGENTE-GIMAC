package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.ImportedStatementTestSamples.*;
import static cm.gimac.des.rapprochement.domain.ReconciliationTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ReconciliationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Reconciliation.class);
        Reconciliation reconciliation1 = getReconciliationSample1();
        Reconciliation reconciliation2 = new Reconciliation();
        assertThat(reconciliation1).isNotEqualTo(reconciliation2);

        reconciliation2.setId(reconciliation1.getId());
        assertThat(reconciliation1).isEqualTo(reconciliation2);

        reconciliation2 = getReconciliationSample2();
        assertThat(reconciliation1).isNotEqualTo(reconciliation2);
    }

    @Test
    void switchStatementTest() {
        Reconciliation reconciliation = getReconciliationRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        reconciliation.setSwitchStatement(importedStatementBack);
        assertThat(reconciliation.getSwitchStatement()).isEqualTo(importedStatementBack);

        reconciliation.switchStatement(null);
        assertThat(reconciliation.getSwitchStatement()).isNull();
    }

    @Test
    void participantStatementTest() {
        Reconciliation reconciliation = getReconciliationRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        reconciliation.setParticipantStatement(importedStatementBack);
        assertThat(reconciliation.getParticipantStatement()).isEqualTo(importedStatementBack);

        reconciliation.participantStatement(null);
        assertThat(reconciliation.getParticipantStatement()).isNull();
    }

    @Test
    void settlementStatementTest() {
        Reconciliation reconciliation = getReconciliationRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        reconciliation.setSettlementStatement(importedStatementBack);
        assertThat(reconciliation.getSettlementStatement()).isEqualTo(importedStatementBack);

        reconciliation.settlementStatement(null);
        assertThat(reconciliation.getSettlementStatement()).isNull();
    }
}
