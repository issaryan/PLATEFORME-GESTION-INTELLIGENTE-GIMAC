package cm.gimac.des.reporting.domain;

import static cm.gimac.des.reporting.domain.ImportedStatementTestSamples.*;
import static cm.gimac.des.reporting.domain.ImportedStatementTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.reporting.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ImportedStatementTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ImportedStatement.class);
        ImportedStatement importedStatement1 = getImportedStatementSample1();
        ImportedStatement importedStatement2 = new ImportedStatement();
        assertThat(importedStatement1).isNotEqualTo(importedStatement2);

        importedStatement2.setId(importedStatement1.getId());
        assertThat(importedStatement1).isEqualTo(importedStatement2);

        importedStatement2 = getImportedStatementSample2();
        assertThat(importedStatement1).isNotEqualTo(importedStatement2);
    }

    @Test
    void supersededByTest() {
        ImportedStatement importedStatement = getImportedStatementRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        importedStatement.setSupersededBy(importedStatementBack);
        assertThat(importedStatement.getSupersededBy()).isEqualTo(importedStatementBack);

        importedStatement.supersededBy(null);
        assertThat(importedStatement.getSupersededBy()).isNull();
    }

    @Test
    void importedStatementTest() {
        ImportedStatement importedStatement = getImportedStatementRandomSampleGenerator();
        ImportedStatement importedStatementBack = getImportedStatementRandomSampleGenerator();

        importedStatement.setImportedStatement(importedStatementBack);
        assertThat(importedStatement.getImportedStatement()).isEqualTo(importedStatementBack);
        assertThat(importedStatementBack.getSupersededBy()).isEqualTo(importedStatement);

        importedStatement.importedStatement(null);
        assertThat(importedStatement.getImportedStatement()).isNull();
        assertThat(importedStatementBack.getSupersededBy()).isNull();
    }
}
