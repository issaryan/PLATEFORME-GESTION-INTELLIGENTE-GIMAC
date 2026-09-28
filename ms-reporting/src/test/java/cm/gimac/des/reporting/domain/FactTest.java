package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.FactTestSamples.*;
import static cm.gimac.des.reporting.domain.ImportedStatementTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class FactTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Fact.class);
        Fact fact1 = getFactSample1();
        Fact fact2 = new Fact();
        assertThat(fact1).isNotEqualTo(fact2);

        fact2.setId(fact1.getId());
        assertThat(fact1).isEqualTo(fact2);

        fact2 = getFactSample2();
        assertThat(fact1).isNotEqualTo(fact2);
    }

    @Test
    void statementTest() {
        Fact fact = getFactRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        fact.setStatement(importedStatementBack);
        assertThat(fact.getStatement()).isEqualTo(importedStatementBack);

        fact.statement(null);
        assertThat(fact.getStatement()).isNull();
    }
}
