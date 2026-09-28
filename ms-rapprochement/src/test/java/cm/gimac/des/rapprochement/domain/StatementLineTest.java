package cm.gimac.des.rapprochement.domain;

import static cm.gimac.des.rapprochement.domain.ImportedStatementTestSamples.*;
import static cm.gimac.des.rapprochement.domain.StatementLineTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.rapprochement.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StatementLineTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StatementLine.class);
        StatementLine statementLine1 = getStatementLineSample1();
        StatementLine statementLine2 = new StatementLine();
        assertThat(statementLine1).isNotEqualTo(statementLine2);

        statementLine2.setId(statementLine1.getId());
        assertThat(statementLine1).isEqualTo(statementLine2);

        statementLine2 = getStatementLineSample2();
        assertThat(statementLine1).isNotEqualTo(statementLine2);
    }

    @Test
    void statementTest() {
        StatementLine statementLine = getStatementLineRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        statementLine.setStatement(importedStatementBack);
        assertThat(statementLine.getStatement()).isEqualTo(importedStatementBack);

        statementLine.statement(null);
        assertThat(statementLine.getStatement()).isNull();
    }
}
